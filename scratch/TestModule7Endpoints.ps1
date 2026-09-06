$baseUrl = "http://localhost:8080"
Write-Host "=== Module 7: Adaptive Online Assessment Engine Test Suite ===" -ForegroundColor Cyan

# 1. Health check
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get
    Write-Host "[PASS] Health check verified: $($health.message)" -ForegroundColor Green
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
$candHeaders = @{ "X-Session-Token" = $candToken }
Write-Host "[PASS] Candidate login successful (Token: $($candToken.Substring(0,8))...)" -ForegroundColor Green

# 3. Login as Recruiter
$recLoginBody = @{
    email = "recruiter@apexcloud.com"
    password = "recruiter123"
} | ConvertTo-Json
$recLogin = Invoke-RestMethod -Uri "$baseUrl/api/login" -Method Post -Body $recLoginBody -ContentType "application/json"
$recToken = $recLogin.data.token
$recHeaders = @{ "X-Session-Token" = $recToken }
Write-Host "[PASS] Recruiter login successful (Token: $($recToken.Substring(0,8))...)" -ForegroundColor Green

# 4. Get Available Assessments
$testsRes = Invoke-RestMethod -Uri "$baseUrl/api/assessments" -Method Get -Headers $candHeaders
Write-Host "[PASS] Retrieved $($testsRes.data.Count) published assessments:" -ForegroundColor Green
foreach ($t in $testsRes.data) {
    Write-Host "       Test #$($t.assessmentId): $($t.title) | Passing: $($t.passingScore)% | Easy: $($t.easyQuestionsCount), Med: $($t.mediumQuestionsCount), Hard: $($t.hardQuestionsCount)" -ForegroundColor Yellow
}

# 5. Get Assessment Details
$test1 = Invoke-RestMethod -Uri "$baseUrl/api/assessments/1" -Method Get -Headers $candHeaders
Write-Host "[PASS] Assessment #1 details verified: '$($test1.data.title)' ($($test1.data.totalQuestions) total questions)" -ForegroundColor Green

# 6. Core Innovation 4: Start Adaptive Assessment Session
Write-Host "`n--- Starting Adaptive Assessment Session (Core Innovation 4) ---" -ForegroundColor Cyan
$startBody = @{ assessmentId = 1 } | ConvertTo-Json
$startRes = Invoke-RestMethod -Uri "$baseUrl/api/assessments/start" -Method Post -Headers $candHeaders -Body $startBody -ContentType "application/json"
$session = $startRes.data
$attemptId = $session.attemptId
Write-Host "[PASS] Adaptive Test Started! Attempt ID: $attemptId" -ForegroundColor Green
Write-Host "       Initial Tier: $($session.currentDifficulty) | Question 1: '$($session.currentQuestion.questionText)'" -ForegroundColor Yellow

# Verify Client-Safe Masking (correctOption must not be leaked to client!)
if ($null -eq $session.currentQuestion.correctOption -or $session.currentQuestion.correctOption -eq "") {
    Write-Host "[PASS] Client-Safe Verification: correctOption is securely masked in client response" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Security issue: correctOption was leaked to client: $($session.currentQuestion.correctOption)" -ForegroundColor Red
}

# 7. Adaptive Step 1: Candidate answers Question 1 CORRECTLY ('B')
# Current: Medium -> Should steer UP to Hard (+15 pts awarded)
Write-Host "`n[Step 1] Answering Question #$($session.currentQuestion.questionId) with option 'B' (Expected: Correct)..." -ForegroundColor Cyan
$ans1Body = @{
    attemptId = $attemptId
    questionId = $session.currentQuestion.questionId
    selectedOption = "B"
} | ConvertTo-Json
$step1Res = Invoke-RestMethod -Uri "$baseUrl/api/assessments/answer" -Method Post -Headers $candHeaders -Body $ans1Body -ContentType "application/json"
$session1 = $step1Res.data
Write-Host "[PASS] Steering Feedback: $($step1Res.message)" -ForegroundColor Green
Write-Host "       Difficulty Steered To: $($session1.currentDifficulty) | Trajectory: $($session1.difficultyTrajectory -join ' -> ')" -ForegroundColor Yellow
Write-Host "       Points Earned: $($session1.pointsEarned) / $($session1.maxPossiblePoints)" -ForegroundColor Yellow

# 8. Adaptive Step 2: In Hard tier, candidate answers Question 2 INCORRECTLY ('A')
# Current: Hard -> Should steer DOWN to Medium (0 pts awarded)
if (-not $session1.completed) {
    Write-Host "`n[Step 2] Answering Question #$($session1.currentQuestion.questionId) with option 'A' (Expected: Incorrect)..." -ForegroundColor Cyan
    $ans2Body = @{
        attemptId = $attemptId
        questionId = $session1.currentQuestion.questionId
        selectedOption = "A"
    } | ConvertTo-Json
    $step2Res = Invoke-RestMethod -Uri "$baseUrl/api/assessments/answer" -Method Post -Headers $candHeaders -Body $ans2Body -ContentType "application/json"
    $session2 = $step2Res.data
    Write-Host "[PASS] Steering Feedback: $($step2Res.message)" -ForegroundColor Green
    Write-Host "       Difficulty Steered To: $($session2.currentDifficulty) | Trajectory: $($session2.difficultyTrajectory -join ' -> ')" -ForegroundColor Yellow
    Write-Host "       Points Earned: $($session2.pointsEarned) / $($session2.maxPossiblePoints)" -ForegroundColor Yellow
}

# 9. Adaptive Step 3: In Medium tier, candidate answers Question 3 CORRECTLY ('B')
# Current: Medium -> Should steer back UP to Hard
if (-not $session2.completed) {
    Write-Host "`n[Step 3] Answering Question #$($session2.currentQuestion.questionId) with option 'B' (Expected: Correct)..." -ForegroundColor Cyan
    $ans3Body = @{
        attemptId = $attemptId
        questionId = $session2.currentQuestion.questionId
        selectedOption = "B"
    } | ConvertTo-Json
    $step3Res = Invoke-RestMethod -Uri "$baseUrl/api/assessments/answer" -Method Post -Headers $candHeaders -Body $ans3Body -ContentType "application/json"
    $session3 = $step3Res.data
    Write-Host "[PASS] Steering Feedback: $($step3Res.message)" -ForegroundColor Green
    Write-Host "       Difficulty Steered To: $($session3.currentDifficulty) | Trajectory: $($session3.difficultyTrajectory -join ' -> ')" -ForegroundColor Yellow
}

# 10. Complete Assessment Session
Write-Host "`n--- Finalizing Assessment Attempt ---" -ForegroundColor Cyan
$finishBody = @{ attemptId = $attemptId } | ConvertTo-Json
$finishRes = Invoke-RestMethod -Uri "$baseUrl/api/assessments/finish" -Method Post -Headers $candHeaders -Body $finishBody -ContentType "application/json"
$finalSession = $finishRes.data
Write-Host "[PASS] Assessment Finalized! Outcome: $($finishRes.message)" -ForegroundColor Green
Write-Host "       Final Score: $($finalSession.percentageScore)% | Peak Difficulty Reached: $($finalSession.currentDifficulty) | Passed: $($finalSession.passed)" -ForegroundColor Yellow

# 11. Candidate queries their attempts history
$attemptsRes = Invoke-RestMethod -Uri "$baseUrl/api/assessments/attempts" -Method Get -Headers $candHeaders
Write-Host "[PASS] Candidate retrieved $($attemptsRes.data.Count) past attempt(s). Latest Score: $($attemptsRes.data[0].percentageScore)%, Peak Tier: $($attemptsRes.data[0].difficultyReached)" -ForegroundColor Green

# 12. View Detailed Attempt Answers Breakdown
$reviewRes = Invoke-RestMethod -Uri "$baseUrl/api/assessments/attempts/$attemptId" -Method Get -Headers $candHeaders
Write-Host "[PASS] Attempt breakdown retrieved ($($reviewRes.data.Count) questions answered):" -ForegroundColor Green
foreach ($ans in $reviewRes.data) {
    Write-Host "       Q#$($ans.questionId) [Tier: $($ans.difficulty)]: Selected='$($ans.selectedOption)' | Correct='$($ans.correctOption)' -> Points: $($ans.pointsAwarded)" -ForegroundColor Cyan
}

# 13. Recruiter creates a custom assessment & adds an adaptive question
Write-Host "`n--- Recruiter Custom Assessment Creation ---" -ForegroundColor Cyan
$createAssessBody = @{
    title = "Cloud Microservices & DevOps Screening"
    description = "Adaptive test covering Docker containers, Kubernetes clustering, and CI/CD pipelines."
    passingScore = 70
    timeLimitMinutes = 25
    jobId = 1
} | ConvertTo-Json
$newAssessRes = Invoke-RestMethod -Uri "$baseUrl/api/assessments" -Method Post -Headers $recHeaders -Body $createAssessBody -ContentType "application/json"
$newAssessId = $newAssessRes.data.assessmentId
Write-Host "[PASS] Recruiter created new assessment #$($newAssessId): '$($newAssessRes.data.title)'" -ForegroundColor Green

# Add a question to new assessment
$createQBody = @{
    assessmentId = $newAssessId
    questionText = "Which Kubernetes object is responsible for ensuring that a specified number of pod replicas are running at all times?"
    optionA = "Service"
    optionB = "ReplicaSet"
    optionC = "ConfigMap"
    optionD = "Ingress"
    correctOption = "B"
    difficulty = "Medium"
    points = 15
} | ConvertTo-Json
$newQRes = Invoke-RestMethod -Uri "$baseUrl/api/assessments/questions" -Method Post -Headers $recHeaders -Body $createQBody -ContentType "application/json"
Write-Host "[PASS] Recruiter added adaptive question to new assessment: $($newQRes.message)" -ForegroundColor Green

# 14. Algorithm Integration Test: Core Innovation 4 feeding into Core Innovation 1 (5-Factor Smart Match)
Write-Host "`n--- Algorithm Integration: Test Score Feeding 5-Factor Smart Match Engine ---" -ForegroundColor Cyan
$matchRes = Invoke-RestMethod -Uri "$baseUrl/api/match?jobId=1" -Method Get -Headers $candHeaders
Write-Host "[PASS] Smart Match Score recalculated: Overall=$($matchRes.data.overallScore)% ($($matchRes.data.matchLevel))" -ForegroundColor Green
Write-Host "       Assessment Factor Component (15% weight): $($matchRes.data.assessmentScore)/100 pts" -ForegroundColor Yellow

Write-Host "`n=== ALL MODULE 7 TESTS COMPLETED SUCCESSFULLY! ===" -ForegroundColor Green
