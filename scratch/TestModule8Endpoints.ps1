# ==============================================================================
# TestModule8Endpoints.ps1
# Automated Integration Test Suite for Module 8: Career Path Recommendation Engine
# Pure PowerShell (Windows)
# ==============================================================================

$baseUrl = "http://localhost:8080"
$passCount = 0
$failCount = 0

function Assert-Test {
    param(
        [string]$testName,
        [bool]$condition,
        [string]$details = ""
    )
    if ($condition) {
        Write-Host "[PASS] $testName" -ForegroundColor Green
        $global:passCount++
    } else {
        Write-Host "[FAIL] $testName - $details" -ForegroundColor Red
        $global:failCount++
    }
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "   INTEGRATION TEST SUITE: MODULE 8 (CAREER PATH ENGINE)" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Health check
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get
    Assert-Test "1. Server Health Check" ($health.success -eq $true) "Health status: $($health.status)"
} catch {
    Assert-Test "1. Server Health Check" $false "Server is unreachable: $_"
    exit 1
}

# 2. Candidate Login (john.doe@email.com)
$candToken = ""
try {
    $loginBody = @{
        email = "john.doe@email.com"
        password = "candidate123"
    } | ConvertTo-Json

    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $loginBody -ContentType "application/json"
    $candToken = $loginRes.data.token
    Assert-Test "2. Candidate Login (john.doe@email.com)" ($loginRes.success -eq $true -and $candToken.Length -gt 10) "Token: $candToken"
} catch {
    Assert-Test "2. Candidate Login" $false "Login failed: $_"
}

# 3. GET /api/career-paths (Authenticated Candidate Trajectory Synthesis)
$candRecs = $null
try {
    $headers = @{ "X-Session-Token" = $candToken }
    $recRes = Invoke-RestMethod -Uri "$baseUrl/api/career-paths" -Method Get -Headers $headers
    $candRecs = $recRes.data.recommendations
    Assert-Test "3. Authenticated Candidate Trajectory Synthesis" ($recRes.success -eq $true -and $candRecs.Count -ge 4) "Count: $($candRecs.Count)"
} catch {
    Assert-Test "3. Authenticated Candidate Trajectory Synthesis" $false "Request failed: $_"
}

# 4. Top Recommendation Validation
if ($candRecs -and $candRecs.Count -gt 0) {
    $top = $candRecs[0]
    $matchScore = $top.matchScore
    $rating = $top.readinessRating
    $title = $top.careerPath.title
    Assert-Test "4. Top Recommendation Score Valid (0-100)" ($matchScore -ge 50 -and $matchScore -le 100) "Score: $matchScore%, Track: $title, Rating: $rating"
    
    # 5. Multi-Factor Breakdown Check
    $sScore = $top.skillsScore
    $eScore = $top.experienceScore
    $pScore = $top.projectScore
    $aScore = $top.assessmentScore
    $edScore = $top.educationScore
    $factorsValid = ($sScore -gt 0 -and $eScore -gt 0 -and $pScore -gt 0 -and $aScore -gt 0 -and $edScore -gt 0)
    Assert-Test "5. 5-Factor Score Breakdown Present" $factorsValid "Skills: $sScore, Exp: $eScore, Proj: $pScore, Assess: $aScore, Edu: $edScore"

    # 6. Milestone Ladder & Level Placement
    $currLevel = $top.currentMilestoneLevel
    $currMilestone = $top.currentMilestone
    $nextMilestone = $top.nextMilestone
    $milestoneValid = ($currLevel -ge 1 -and $currLevel -le 4 -and $currMilestone -ne $null)
    Assert-Test "6. Candidate Placed on Milestone Ladder" $milestoneValid "Current: L$currLevel ($($currMilestone.levelName))"

    # 7. Next Frontier Skills Discovered
    $nextSkills = $top.nextFrontierSkills
    Assert-Test "7. Next Frontier Skills Identified" ($null -ne $nextSkills) "Next Skills: $($nextSkills -join ', ')"

    # 8. Stepping-Stone Active Vacancies Linked
    $jobs = $top.matchingJobs
    Assert-Test "8. Stepping-Stone Active Vacancies Found" ($jobs -ne $null -and $jobs.Count -ge 1) "Matching Jobs: $($jobs.Count)"
} else {
    Assert-Test "4-8. Top Recommendation Tests" $false "Recommendations array was empty"
}

# 9. GET /api/career-paths/1 (Detailed Track with 4 Progressive Milestones)
try {
    $trackRes = Invoke-RestMethod -Uri "$baseUrl/api/career-paths/1" -Method Get -Headers @{ "X-Session-Token" = $candToken }
    $track = $trackRes.data.careerPath
    if (-not $track) { $track = $trackRes.data }
    $milestones = $track.milestones
    Assert-Test "9. Detailed Track Retrieval with Milestones" ($trackRes.success -eq $true -and $milestones.Count -eq 4) "Milestones count: $($milestones.Count)"
} catch {
    Assert-Test "9. Detailed Track Retrieval with Milestones" $false "Request failed: $_"
}

# 10. Public / Unauthenticated GET /api/career-paths (Returns 4 Tracks)
try {
    $pubRes = Invoke-RestMethod -Uri "$baseUrl/api/career-paths" -Method Get
    $tracks = $pubRes.data
    Assert-Test "10. Public Unauthenticated Career Tracks List" ($pubRes.success -eq $true -and $tracks.Count -ge 4) "Public Tracks Count: $($tracks.Count)"
} catch {
    Assert-Test "10. Public Unauthenticated Career Tracks List" $false "Request failed: $_"
}

# 11. Admin Login & Track Creation (RBAC)
$adminToken = ""
try {
    $adminLogin = @{
        email = "admin@recruithub.com"
        password = "admin123"
    } | ConvertTo-Json

    $admRes = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $adminLogin -ContentType "application/json"
    $adminToken = $admRes.data.token
    Assert-Test "11. Admin Login (admin@recruithub.com)" ($admRes.success -eq $true -and $adminToken.Length -gt 10) "Token: $adminToken"
} catch {
    Assert-Test "11. Admin Login" $false "Login failed: $_"
}

# 12. Admin POST /api/career-paths (Create Custom Track)
$newPathId = 0
try {
    $newTrackBody = @{
        title = "Cybersecurity & Cloud Defense Architect $(Get-Random)"
        category = "Cybersecurity & Infrastructure"
        description = "Specializes in zero-trust networks, cloud defense postures, and cryptographic safeguards."
        requiredCoreSkills = "Linux, Networking, Docker, AWS, Cryptography"
        minStartingExperienceYears = 1
        averageMarketSalary = "$125,000 - $195,000"
    } | ConvertTo-Json

    $createRes = Invoke-RestMethod -Uri "$baseUrl/api/career-paths" -Method Post -Body $newTrackBody -ContentType "application/json" -Headers @{ "X-Session-Token" = $adminToken }
    $newPathId = $createRes.data.pathId
    Assert-Test "12. Admin POST /api/career-paths (Create Track)" ($createRes.success -eq $true -and $newPathId -gt 0) "Created Track ID: $newPathId"
} catch {
    Assert-Test "12. Admin POST /api/career-paths" $false "Failed: $_"
}

# 13. Admin POST /api/career-paths/milestones (Add Milestone)
if ($newPathId -gt 0) {
    try {
        $newMilestoneBody = @{
            pathId = $newPathId
            levelOrder = 1
            levelName = "Junior SOC & Cloud Defense Analyst (L1)"
            experienceYearsRange = "0-2 Years"
            salaryRange = "$80,000 - $100,000"
            skillsRequired = "Linux, Networking, Bash"
            milestoneDescription = "Monitor perimeter logs, remediate vulnerability scans, and configure basic firewalls."
            recommendedAction = "Attain CompTIA Security+ or AWS Certified Security Specialty."
        } | ConvertTo-Json

        $mRes = Invoke-RestMethod -Uri "$baseUrl/api/career-paths/milestones" -Method Post -Body $newMilestoneBody -ContentType "application/json" -Headers @{ "X-Session-Token" = $adminToken }
        Assert-Test "13. Admin POST /api/career-paths/milestones (Add Milestone)" ($mRes.success -eq $true -and $mRes.data.milestoneId -gt 0) "Milestone ID: $($mRes.data.milestoneId)"
    } catch {
        Assert-Test "13. Admin POST /api/career-paths/milestones" $false "Failed: $_"
    }
} else {
    Assert-Test "13. Admin POST /api/career-paths/milestones" $false "Skipped due to track creation failure"
}

# 14. Candidate RBAC Protection (Candidate Cannot Create Track)
try {
    $unauthBody = @{
        title = "Hacker Track"
        requiredCoreSkills = "None"
    } | ConvertTo-Json

    $badRes = Invoke-RestMethod -Uri "$baseUrl/api/career-paths" -Method Post -Body $unauthBody -ContentType "application/json" -Headers @{ "X-Session-Token" = $candToken }
    Assert-Test "14. Candidate RBAC Protection (POST /api/career-paths)" $false "Should have failed with 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "14. Candidate RBAC Protection (POST /api/career-paths)" ($statusCode -eq 403) "Correctly returned HTTP $statusCode Forbidden"
}

# 15. Verify Static File Serving for career-path.html
try {
    $page = Invoke-WebRequest -Uri "$baseUrl/applicant/career-path.html" -Method Get -UseBasicParsing
    Assert-Test "15. Static Delivery of /applicant/career-path.html" ($page.StatusCode -eq 200 -and $page.Content.Contains("Career Path Recommendation Engine")) "HTTP 200 OK"
} catch {
    Assert-Test "15. Static Delivery of /applicant/career-path.html" $false "Failed: $_"
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " TEST RESULTS SUMMARY: PASS = $passCount, FAIL = $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "==================================================================" -ForegroundColor Cyan

if ($failCount -eq 0) {
    exit 0
} else {
    exit 1
}
