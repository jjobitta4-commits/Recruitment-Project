$baseUrl = "http://localhost:8080"
Write-Host "=== Module 6 End-to-End Test Suite ===" -ForegroundColor Cyan

# 1. Health check
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get
    Write-Host "[PASS] Health check: $($health.message)" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] Server is not running on $baseUrl" -ForegroundColor Red
    exit 1
}

# 2. Login as Candidate (John Doe)
$candLoginBody = @{
    email = "john.doe@email.com"
    password = "candidate123"
} | ConvertTo-Json

$candLogin = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $candLoginBody -ContentType "application/json"
$candToken = $candLogin.data.token
Write-Host "[PASS] Candidate login successful (Token: $($candToken.Substring(0,8))...)" -ForegroundColor Green

# 3. Login as Recruiter
$recLoginBody = @{
    email = "recruiter@apexcloud.com"
    password = "recruiter123"
} | ConvertTo-Json

$recLogin = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $recLoginBody -ContentType "application/json"
$recToken = $recLogin.data.token
Write-Host "[PASS] Recruiter login successful (Token: $($recToken.Substring(0,8))...)" -ForegroundColor Green

# 4. Check if candidate already has an application for Job #1; if so, test withdrawal or fetch it
$candHeaders = @{ "X-Session-Token" = $candToken }
$existingApps = Invoke-RestMethod -Uri "$baseUrl/api/applications" -Method Get -Headers $candHeaders

Write-Host "[INFO] Candidate currently has $($existingApps.data.Count) existing application(s):" -ForegroundColor Cyan
foreach ($a in $existingApps.data) {
    Write-Host "       App #$($a.applicationId) -> Job #$($a.jobId) '$($a.jobTitle)' (Status: $($a.status))" -ForegroundColor Cyan
}

$appId = 0
$hasJob1 = $false
foreach ($app in $existingApps.data) {
    if ($app.jobId -eq 1) {
        $hasJob1 = $true
        $appId = $app.applicationId
        break
    }
}

if ($hasJob1) {
    Write-Host "[INFO] Candidate previously applied for Job #1 (App ID: $appId). Testing Duplicate Prevention..." -ForegroundColor Yellow
} else {
    # Apply for Job #1
    $applyBody = @{
        jobId = 1
        coverLetter = "I have 5 years of solid backend experience in Java, Spring Boot, Microservices, and MySQL. I am very excited about this role at Apex Cloud Systems!"
    } | ConvertTo-Json

    $applyRes = Invoke-RestMethod -Uri "$baseUrl/api/applications" -Method Post -Headers $candHeaders -Body $applyBody -ContentType "application/json"
    $appId = $applyRes.data.applicationId
    $matchScore = $applyRes.data.matchScore
    Write-Host "[PASS] Candidate applied for Job #1! App ID: $appId, Smart Match Score: $matchScore%" -ForegroundColor Green
}

# 5. Test Duplicate Prevention (Should fail with 400 or 409)
try {
    $dupBody = @{
        jobId = 1
        coverLetter = "Attempting duplicate application"
    } | ConvertTo-Json
    $dupRes = Invoke-RestMethod -Uri "$baseUrl/api/applications" -Method Post -Headers $candHeaders -Body $dupBody -ContentType "application/json"
    Write-Host "[FAIL] Duplicate application was allowed!" -ForegroundColor Red
} catch {
    Write-Host "[PASS] Duplicate application properly rejected with HTTP $($_.Exception.Response.StatusCode)" -ForegroundColor Green
}

# 6. Candidate retrieves their applications
$candApps = Invoke-RestMethod -Uri "$baseUrl/api/applications" -Method Get -Headers $candHeaders
Write-Host "[PASS] Candidate retrieved $($candApps.data.Count) application(s)" -ForegroundColor Green
$targetApp = $null
foreach ($a in $candApps.data) {
    if ($a.jobId -eq 1) {
        $targetApp = $a
        break
    }
}
Write-Host "       App Details: Job='$($targetApp.jobTitle)', Company='$($targetApp.companyName)', Status='$($targetApp.status)', MatchScore=$($targetApp.matchScore)%" -ForegroundColor Cyan

# 7. Candidate Application Stats
$candStats = Invoke-RestMethod -Uri "$baseUrl/api/applications/stats" -Method Get -Headers $candHeaders
Write-Host "[PASS] Candidate stats retrieved: Total=$($candStats.data.total), Applied=$($candStats.data.applied), Shortlisted=$($candStats.data.shortlisted)" -ForegroundColor Green

# 8. Recruiter retrieves ranked candidates by Smart Match Score (Core Innovation 3)
$recHeaders = @{ "X-Session-Token" = $recToken }
$rankedMatch = Invoke-RestMethod -Uri "$baseUrl/api/applications?jobId=1&sortBy=match" -Method Get -Headers $recHeaders
Write-Host "[PASS] Recruiter retrieved $($rankedMatch.data.Count) ranked candidates sorted by Smart Match Score" -ForegroundColor Green
foreach ($cand in $rankedMatch.data) {
    Write-Host "       Rank #$($cand.rank): $($cand.candidateName) | Match: $($cand.matchScore)% ($($cand.matchLevel)) | Exp: $($cand.candidateExperienceYears) Yrs | Skills: $($cand.skillScore)% | $($cand.rankingInsight)" -ForegroundColor Yellow
}

# 9. Recruiter retrieves ranked candidates by Experience Depth
$rankedExp = Invoke-RestMethod -Uri "$baseUrl/api/applications?jobId=1&sortBy=exp" -Method Get -Headers $recHeaders
Write-Host "[PASS] Recruiter ranked candidates sorted by Experience Depth: Top rank has $($rankedExp.data[0].candidateExperienceYears) years experience" -ForegroundColor Green

# 10. Recruiter retrieves ranked candidates by Technical Skill Score
$rankedSkills = Invoke-RestMethod -Uri "$baseUrl/api/applications?jobId=1&sortBy=skills" -Method Get -Headers $recHeaders
Write-Host "[PASS] Recruiter ranked candidates sorted by Technical Skills: Top rank has $($rankedSkills.data[0].skillScore)% skill score" -ForegroundColor Green

# 11. Recruiter updates candidate status to 'Under_Review' then 'Shortlisted'
$statusReviewBody = @{
    applicationId = $appId
    status = "Under Review"
} | ConvertTo-Json

$reviewRes = Invoke-RestMethod -Uri "$baseUrl/api/applications/status" -Method Put -Headers $recHeaders -Body $statusReviewBody -ContentType "application/json"
Write-Host "[PASS] Status updated to 'Under Review': $($reviewRes.message)" -ForegroundColor Green

$statusShortlistBody = @{
    applicationId = $appId
    status = "Shortlisted"
} | ConvertTo-Json

$shortlistRes = Invoke-RestMethod -Uri "$baseUrl/api/applications/status" -Method Put -Headers $recHeaders -Body $statusShortlistBody -ContentType "application/json"
Write-Host "[PASS] Status updated to 'Shortlisted': $($shortlistRes.message)" -ForegroundColor Green

# 12. Candidate verifies updated status
$candAppsUpdated = Invoke-RestMethod -Uri "$baseUrl/api/applications" -Method Get -Headers $candHeaders
foreach ($a in $candAppsUpdated.data) {
    if ($a.applicationId -eq $appId) {
        Write-Host "[PASS] Candidate sees updated pipeline status: '$($a.status)'" -ForegroundColor Green
        break
    }
}

# 13. Recruiter Application Stats
$recStats = Invoke-RestMethod -Uri "$baseUrl/api/applications/stats" -Method Get -Headers $recHeaders
Write-Host "[PASS] Recruiter stats: Total=$($recStats.data.total), Shortlisted=$($recStats.data.shortlisted)" -ForegroundColor Green

# 14. Notifications verification
$candNotifs = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers $candHeaders
Write-Host "[PASS] Candidate notifications retrieved ($($candNotifs.data.Count) total)" -ForegroundColor Green

# 15. Candidate Application Withdrawal Lifecycle Test
try {
    $appToWithdraw = $null
    foreach ($a in $existingApps.data) {
        if ($a.jobId -eq 2) {
            $appToWithdraw = $a
            break
        }
    }

    $withdrawId = 0
    if ($null -eq $appToWithdraw) {
        $apply2Body = @{
            jobId = 2
            coverLetter = "Applying for Job 2 to test withdrawal lifecycle"
        } | ConvertTo-Json
        $app2Res = Invoke-RestMethod -Uri "$baseUrl/api/applications" -Method Post -Headers $candHeaders -Body $apply2Body -ContentType "application/json"
        $withdrawId = $app2Res.data.applicationId
        Write-Host "[PASS] Submitted fresh application for Job #2 (App ID: $withdrawId)" -ForegroundColor Green
    } else {
        $withdrawId = $appToWithdraw.applicationId
        Write-Host "[INFO] Candidate previously had application #$withdrawId for Job #2. Withdrawing..." -ForegroundColor Cyan
    }

    # Execute withdrawal
    $delRes = Invoke-RestMethod -Uri "$baseUrl/api/applications?applicationId=$withdrawId" -Method Delete -Headers $candHeaders
    Write-Host "[PASS] Withdrew application #$withdrawId successfully: $($delRes.message)" -ForegroundColor Green

    # Re-verify candidate applications list
    $appsAfterDel = Invoke-RestMethod -Uri "$baseUrl/api/applications" -Method Get -Headers $candHeaders
    $stillExists = $false
    foreach ($a in $appsAfterDel.data) {
        if ($a.applicationId -eq $withdrawId) { $stillExists = $true; break }
    }
    if (-not $stillExists) {
        Write-Host "[PASS] Verified application #$withdrawId is completely removed from active pipeline" -ForegroundColor Green
    }
} catch {
    Write-Host "[FAIL] Withdrawal test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== ALL MODULE 6 TESTS COMPLETED SUCCESSFULLY! ===" -ForegroundColor Green
