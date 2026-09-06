# ==============================================================================
# TestAdminExclusivityAndEmptyState.ps1
# Automated Verification of Clean Recruiter/Applicant State & Admin Exclusivity
# ==============================================================================

$baseUrl = "http://localhost:8080"
$pass = 0
$fail = 0

function Assert-Check {
    param([string]$name, [bool]$cond, [string]$msg = "")
    if ($cond) {
        Write-Host "[PASS] $name" -ForegroundColor Green
        $global:pass++
    } else {
        Write-Host "[FAIL] $name - $msg" -ForegroundColor Red
        $global:fail++
    }
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  VERIFYING CLEAN DATABASE STATE & ADMIN EXCLUSIVITY" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Health check
try {
    $h = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get
    Assert-Check "1. Server Health Check" ($h.success -eq $true) "Status: $($h.status)"
} catch {
    Assert-Check "1. Server Health Check" $false "Unreachable: $_"
    exit 1
}

# 2. Admin Login Verification (admin@recruithub.com / admin123)
$adminToken = ""
try {
    $admBody = @{
        email = "admin@recruithub.com"
        password = "admin123"
    } | ConvertTo-Json

    $admRes = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $admBody -ContentType "application/json"
    $adminToken = $admRes.data.token
    $adminRole = $admRes.data.role
    Assert-Check "2. Admin Login (admin@recruithub.com / admin123)" ($admRes.success -eq $true -and $adminRole -eq "admin") "Role: $adminRole"
} catch {
    Assert-Check "2. Admin Login" $false "Login failed: $_"
}

# 3. Old Demo Candidate Login Should FAIL (john.doe@email.com removed)
try {
    $oldCand = @{
        email = "john.doe@email.com"
        password = "candidate123"
    } | ConvertTo-Json

    $candRes = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $oldCand -ContentType "application/json"
    Assert-Check "3. Deleted Candidate Cannot Login" $false "Old candidate still logged in!"
} catch {
    Assert-Check "3. Deleted Candidate Cannot Login" $true "Correctly rejected (User not found/purged)"
}

# 4. Old Demo Recruiter Login Should FAIL (recruiter@apexcloud.com removed)
try {
    $oldRec = @{
        email = "recruiter@apexcloud.com"
        password = "recruiter123"
    } | ConvertTo-Json

    $recRes = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $oldRec -ContentType "application/json"
    Assert-Check "4. Deleted Recruiter Cannot Login" $false "Old recruiter still logged in!"
} catch {
    Assert-Check "4. Deleted Recruiter Cannot Login" $true "Correctly rejected (User not found/purged)"
}

# 5. Attempt To Register as Admin Role with Another Email (MUST BE REJECTED)
try {
    $fakeAdmin = @{
        email = "hacker@domain.com"
        password = "password123"
        role = "admin"
        fullName = "Fake Admin"
    } | ConvertTo-Json

    $badRes = Invoke-RestMethod -Uri "$baseUrl/api/register" -Method Post -Body $fakeAdmin -ContentType "application/json"
    Assert-Check "5. Block Register with Admin Role" $false "Server permitted admin self-registration!"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-Check "5. Block Register with Admin Role" ($status -eq 403) "Correctly returned HTTP $status Forbidden"
}

# 6. Attempt To Register using admin@recruithub.com (MUST BE REJECTED)
try {
    $dupAdmin = @{
        email = "admin@recruithub.com"
        password = "anotherpassword"
        role = "candidate"
        fullName = "Duplicate Admin Attempt"
    } | ConvertTo-Json

    $badAdminRes = Invoke-RestMethod -Uri "$baseUrl/api/register" -Method Post -Body $dupAdmin -ContentType "application/json"
    Assert-Check "6. Block Register with Admin Demo Email" $false "Server permitted registering admin email!"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-Check "6. Block Register with Admin Demo Email" ($status -ge 400 -and $status -lt 500) "Correctly returned HTTP $status"
}

# 7. Register a Clean New Candidate (Allowed)
$newCandToken = ""
try {
    $newCand = @{
        email = "newapplicant@example.com"
        password = "applicantpass"
        role = "candidate"
        fullName = "Sarah Applicant"
    } | ConvertTo-Json

    $newCandRes = Invoke-RestMethod -Uri "$baseUrl/api/register" -Method Post -Body $newCand -ContentType "application/json"
    $newCandToken = $newCandRes.data.token
    Assert-Check "7. Register New Candidate Account" ($newCandRes.success -eq $true -and $newCandRes.data.role -eq "candidate") "New candidate registered"
} catch {
    Assert-Check "7. Register New Candidate Account" $false "Failed: $_"
}

# 8. Register a Clean New Recruiter (Allowed)
$newRecToken = ""
try {
    $newRec = @{
        email = "newrecruiter@example.com"
        password = "recruiterpass"
        role = "recruiter"
        fullName = "David Talent"
        companyName = "Apex Cloud Solutions"
        designation = "Lead Recruiter"
    } | ConvertTo-Json

    $newRecRes = Invoke-RestMethod -Uri "$baseUrl/api/register" -Method Post -Body $newRec -ContentType "application/json"
    $newRecToken = $newRecRes.data.token
    Assert-Check "8. Register New Recruiter Account" ($newRecRes.success -eq $true -and $newRecRes.data.role -eq "recruiter") "New recruiter registered"
} catch {
    Assert-Check "8. Register New Recruiter Account" $false "Failed: $_"
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " RESULTS: PASS = $pass, FAIL = $fail" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "==================================================================" -ForegroundColor Cyan

if ($fail -eq 0) { exit 0 } else { exit 1 }
