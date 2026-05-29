$issueKey                   = "CPDEMO-1"
$cesCpUrl                   = 'http://cwcc.bmc.com:2020'
$cesToken                   = '665fc9fb-39de-428a-8a67-a3619752873d'

$jiraUrl                    = 'https://ralphnuesse.atlassian.net'
$jiraToken                  = 'Basic cmFscGgubnVlc3NlQGdtYWlsLmNvbTpBVEFUVDN4RmZHRjBrUHVnaktkRmtTS3cyemxuWHZpbi0yejZKN08zQXR2UEY0WVVsX2RwcHA0Z0dXOEtXWXY2bTVVY1gyU0c5eUNmYVUwTFRBVkQwTjY5LVE4UmVyTEszRW4tdHdFWUNJVmkwaXhOempKcThDakFjWHdua25nN1JYS1dXajcyMl9JU2RGTVJ4X0V4TklZQVJGWTk0Y0VJa09nZFY2YTNmNTd0SmtrOXZFRGpic2s9RjBFQzBGMDY='
$jiraAssignmentIdField      = "customfield_10082"
$jiraCpTaskListField        = "customfield_10084"
$jiraRequiredTaskListField  = "customfield_10150"

$cpStream                   = "FTSDEMO"
$cpApplication              = "RXN3"
$cpSubApp                   = "RXN3"
$cpRuntime                  = "iccga"
$cpAssignmentDefaultPath    = "DEV1"
$issueSummary               = "New Feature"
$cpAssignmentOwner          = "hddrxm0"
$cpAssignmentPrefix         = "RXN3"

$jiraRequiredTaskFields     = $("name", "type", "inAssignment", "qaProm", "qaImpl", "stgProm", "stgImpl", "prdProm", "prdImpl")

Write-Host "Creating new Assignment for Jira Ticket"$issueKey"."

# $requestHeaders = @{}
# $requestHeaders.add("Authorization", $cesToken)
# $requestHeaders.add("Content-Type", "application/json")

# $requestBody  = '{
#     "stream":               '+$cpStream+',
#     "application":          '+$cpApplication+',
#     "subAppl":              '+$cpSubApp+',
#     "runtimeConfiguration": '+$cpRuntime+',
#     "defaultPath":          '+$cpAssignmentDefaultPath+',
#     "description":          '+$issueSummary+',
#     "owner":                '+$cpAssignmentOwner+',
#     "assignmentPrefix":     '+$cpAssignmentPrefix+',
#     "referenceNumber":      '+$issueKey+'
# }'

# $errorMessage = ""
# $restUri      = "$cesCpUrl/ispw/$cpRuntime/assignments"

# Write-Host "Calling Url:"
# Write-Host $restUri
# Write-Host "Using Body:"
# Write-Host $requestBody

# try{
#     $response = Invoke-WebRequest -Method POST -Uri $restUri -Headers $requestHeaders -Body $requestBody -UseBasicParsing
#     $statusCode = $response.StatusCode
#     Write-Host $statusCode
# }
# catch{
#     $statusCode = $_.Exception.Response.StatusCode.value__
#     Write-Host "`nAn error occurred for REST call"
#     Write-Host $statusCode
#     Write-Host $response
#     Exit 1          
# }

# $assignmentInfo   = ConvertFrom-Json $response
# $assignmentId     = $assignmentInfo.assignmentId

$assignmentId = "RXN3000172"
Write-Host "Assignment created: $assignmentId"

Write-Host "Querying Jira Ticket"+$issueKey

$requestHeaders = @{}
$requestHeaders.add("Authorization", $jiraToken)
$requestHeaders.add("Content-Type", "application/json")

$restUri        = "$jiraUrl/rest/api/3/issue/$issueKey"

Write-Host "Calling Url:"
Write-Host $restUri

$response = Invoke-WebRequest -Method GET -Uri $restUri -Headers $requestHeaders -Body $requestBody -UseBasicParsing
$statusCode = $response.StatusCode
Write-Host $statusCode

$ticketInfo     = ConvertFrom-Json $response

$jiraRequiredTaskListTableRows = $ticketInfo.fields.$jiraRequiredTaskListField.content[0].content

$jiraRequiredTasksList = @{}
$rowIndex              = 0
foreach($row in $jiraRequiredTaskListTableRows) {
    if($rowIndex -gt 0) {

        $rowCells       = $row.content
        $requiredTask   = @{}
        $cellIndex      = 0

        foreach($cell in $rowCells) {
            if($cell.content[0].content -eq $null) {
                $requiredTask.add($jiraRequiredTaskFields[$cellIndex], "")
            }
            else {
                $requiredTask.add($jiraRequiredTaskFields[$cellIndex], $cell.content[0].content[0].text.trim())
            }
            $cellIndex = $cellIndex + 1        
        }

        $requiredTaskKey = $requiredTask.name+"_"+$requiredTask.type
        $jiraRequiredTasksList.add($requiredTaskKey, $requiredTask)
    }

    $rowIndex = $rowIndex + 1
}

foreach($task in $jiraRequiredTasksList.values) {
    $taskName = $task.name
    $taskType = $task.type

    Write-Host "Adding Task to Assignment"$assignmentId":" 
    Write-Host $taskName
    Write-Host $taskType
    Write-Host

    $requestHeaders = @{}
    $requestHeaders.add("Authorization", $cesToken)
    $requestHeaders.add("Content-Type", "application/json")

    $requestBody  = '{
        "taskName":             '+$taskName+',
        "type":                 '+$taskType+',
        "stream":               '+$cpStream+',
        "application":          '+$cpApplication+',
        "subAppl":              '+$cpSubApp+',
        "runtimeConfiguration": '+$cpRuntime+',
        "path":                 '+$cpAssignmentDefaultPath+',
        "owner":                '+$cpAssignmentOwner+'
    }'

    $restUri      = "$cesCpUrl/ispw/$cpRuntime/assignments/$assignmentId/task/add"

    Write-Host "Calling Url:"
    Write-Host $restUri
    Write-Host "Using Body:"
    Write-Host $requestBody

    try{
        $response = Invoke-WebRequest -Method POST -Uri $restUri -Headers $requestHeaders -Body $requestBody -UseBasicParsing
        $statusCode = $response.StatusCode
        Write-Host $statusCode
    }
    catch{
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "`nAn error occurred for REST call"
        Write-Host $statusCode
        Write-Host $response
        Exit 1          
    }
}