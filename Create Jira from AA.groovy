/*
Parameters
    abendCode
    abendDate
    abendTime
    abendJobName
    abendJobId
    abendProgram
    abendModule
    abendReportUrl
*/

def jiraAssignmentIdField   = "customfield_10082"
def jiraTaskListField       = "customfield_10084"
def oldJiraTicket
def jiraTicketId

def abendDiagnostics
def programInfo             = [:]

def incidentNotes           = []
def notesIndex              = 0

def jiraDescription         = [:]
jiraDescription.type        = "doc"
jiraDescription.version     = 1

def descriptionParagraphs   = []

def emailRecipient          = 'ralph_nuesse@bmc.com'

echo "Parameters received from Abend-AID Webhook:\n" +
    "  abendCode     : " + abendCode + "\n" +
    "  abendDate     : " + abendDate + "\n" +
    "  abendTime     : " + abendTime + "\n" +
    "  abendJobName  : " + abendJobName + "\n" +
    "  abendJobId    : " + abendJobId + "\n" +
    "  abendProgram  : " + abendProgram + "\n" +
    "  abendModule   : " + abendModule + "\n" +
    "  abendReportUrl: " + abendReportUrl

node{

    stage("Get Abend Info") {

        def response
        def abendReportNum  = abendReportUrl.split('[?]')[1].split('[=]')[1]
        def attempts        = 20
        def reportFound     = false 
        
        while (attempts > 0 & !reportFound) {

            response = httpRequest(
                acceptType: 'APPLICATION_JSON', 
                consoleLogResponseBody: true, 
                contentType: 'APPLICATION_JSON', 
                validResponseCodes: '100:500',
                customHeaders: 
                    [[
                        maskValue: true, 
                        name: 'authorization', 
                        value: cesToken
                    ]], 
                url: cesAaUrl + '/compuware/ws/abendaidapi/diagnosticsummary?data=RPT=' + abendReportNum, 
                wrapAsMultipart: false
            )

            if (response.status < 400) {
                reportFound = true
            }
            else{
                sleep(20)
            }

            attempts--
        }

        if (!reportFound) {
            currentBuild.result = 'ABORTED'
            error('The Abend-AID report with number ' + abendReportNum + 'was not found within the allowed timeframe.\nWe are going to abort this build.')
        }
        
        abendDiagnostics = readJSON(text: response.content)
    }

    stage("Create Jira Description"){

        def textMarksStrong           = []
        textMarksStrong[0]            = [:]
        textMarksStrong[0].type       = "strong"    

        def textMarksLink             = []
        textMarksLink[0]          = [:]
        textMarksLink[0].type     = "link"
        def linkAttributes            = [:]

        def descriptionLine0    = []
        def descriptionLine1    = []
        def descriptionLine2    = []
        def descriptionLine3    = []

        def paragraphIndex      = 0

        descriptionLine0[0]         = [:]
        descriptionLine0[0].text    = 'An abend occured. A '
        descriptionLine0[0].type    = 'text'

        linkAttributes.href         = abendReportUrl
        linkAttributes.title        = 'Abend-AID'
        textMarksLink[0].attrs      = linkAttributes

        descriptionLine0[1]         = [:]
        descriptionLine0[1].text    = 'detailed Abend-AID Report'
        descriptionLine0[1].type    = 'text'
        descriptionLine0[1].marks   = textMarksLink
        
        descriptionLine0[2]         = [:]
        descriptionLine0[2].text    = ' has been created.'
        descriptionLine0[2].type    = 'text'

        descriptionParagraphs[paragraphIndex]           = [:]
        descriptionParagraphs[paragraphIndex].content   = descriptionLine0
        descriptionParagraphs[paragraphIndex].type      = "paragraph"

        paragraphIndex++

        descriptionLine1[0]         = [:]
        descriptionLine1[0].text    = "Following is the Abend-AID Diagnostic Summary."
        descriptionLine1[0].type    = "text"
        descriptionLine1[0].marks   = textMarksStrong

        descriptionParagraphs[paragraphIndex]           = [:]
        descriptionParagraphs[paragraphIndex].content   = descriptionLine1
        descriptionParagraphs[paragraphIndex].type      = "paragraph"

        def exeLines        = []
        def lineIndex       = 0

        abendDiagnostics.diag.exe[0][0].each{

            exeLines[lineIndex]         = [:]
            exeLines[lineIndex].text    = it.value
            exeLines[lineIndex].type    = 'text'
            lineIndex++

        }

        paragraphIndex++

        descriptionParagraphs[paragraphIndex]           = [:]
        descriptionParagraphs[paragraphIndex].content   = exeLines
        descriptionParagraphs[paragraphIndex].type      = "paragraph"

        paragraphIndex++

        descriptionLine2[0]         = [:]
        descriptionLine2[0].text    = "Analysis of the Event:"
        descriptionLine2[0].type    = "text"
        descriptionLine2[0].marks   = textMarksStrong

        descriptionParagraphs[paragraphIndex]           = [:]
        descriptionParagraphs[paragraphIndex].content   = descriptionLine2
        descriptionParagraphs[paragraphIndex].type      = "paragraph"

        abendDiagnostics.diag.aofE[0][0].each{
            
            paragraphIndex++
            def aofELines                       = []

            aofELines[0]                        = [:]
            aofELines[0].text                   = it.value
            aofELines[0].type                   = "text"

            descriptionParagraphs[paragraphIndex]           = [:]
            descriptionParagraphs[paragraphIndex].content   = aofELines
            descriptionParagraphs[paragraphIndex].type      = "paragraph"
            
        }

        paragraphIndex++
        descriptionLine3[0]         = [:]
        descriptionLine3[0].text    = "Program Information:"
        descriptionLine3[0].type    = "text"
        descriptionLine3[0].marks   = textMarksStrong

        descriptionParagraphs[paragraphIndex]           = [:]
        descriptionParagraphs[paragraphIndex].content   = descriptionLine3
        descriptionParagraphs[paragraphIndex].type      = "paragraph"

        abendDiagnostics.diag.pi[0][0].each{

            paragraphIndex++
            def piLines                       = []
            piLines[0]                        = [:]
            piLines[0].text                   = it.value
            piLines[0].type                   = "text"

            descriptionParagraphs[paragraphIndex]           = [:]
            descriptionParagraphs[paragraphIndex].content   = piLines
            descriptionParagraphs[paragraphIndex].type      = "paragraph"

        }       

        jiraDescription.content = descriptionParagraphs

        incidentNotes[notesIndex] = 'An abend occured. The detailed report can be found at ' + abendReportUrl + '.'

        notesIndex++
        incidentNotes[notesIndex] = "Following is the Abend-AID Diagnostic Summary."

        abendDiagnostics.diag.exe[0][0].each{
            notesIndex++
            incidentNotes[notesIndex] = it.value
        }

        notesIndex++        
        incidentNotes[notesIndex] = 'Analysis of the Event'
        
        abendDiagnostics.diag.aofE[0][0].each{
            notesIndex++
            incidentNotes[notesIndex] = it.value
        }

        notesIndex++
        incidentNotes[notesIndex] = 'Program Information'
        
        abendDiagnostics.diag.pi[0][0].each{
            notesIndex++
            incidentNotes[notesIndex] = it.value
        }       
    }

    stage("Create JIRA Ticket") {
        def incidentSummary = 'Abending Program ' + abendProgram + ' with Abend Code ' + abendCode

        def requestBody = [:]

        def fields = [:]
        def project = [:]
        def issueType = [:]

        issueType.id = "10006"
        project.id = "10000"
/*
        def paragraphIndex = 0

        incidentNotes.each{
            def textLines = []
            textLines[0] = [:]
            textLines[0].text = it
            textLines[0].type = "text"

            paragraphs[paragraphIndex] = [:]
            paragraphs[paragraphIndex].content = textLines
            paragraphs[paragraphIndex].type = "paragraph"
            
            paragraphIndex++
        }

        description.content = paragraphs
        description.type = "doc"
        description.version = 1
*/
        def taskListLines = []
        def taskListParagraphs = []
        def taskListDoc = [:]

        taskListLines[0] = [:]
        taskListLines[0].text = "<none>"
        taskListLines[0].type = "text"

        taskListParagraphs[0] = [:]
        taskListParagraphs[0].content = taskListLines
        taskListParagraphs[0].type = "paragraph"

        taskListDoc.content = taskListParagraphs
        taskListDoc.type = "doc"
        taskListDoc.version = 1

        fields.project = project
        fields.summary = incidentSummary
        fields.description = jiraDescription
        fields.issuetype = issueType
        fields."${jiraAssignmentIdField}" =  "<none>"
        fields."${jiraTaskListField}" =  taskListDoc

        requestBody.fields = fields

        def requestBodyJson = writeJSON(returnText: true, json: requestBody)

        echo "Request Body:"
        echo requestBodyJson

        def response = httpRequest(
            customHeaders: [[maskValue: false, name: 'authorization', value: jiraToken]],
            consoleLogResponseBody: true, 
            contentType: 'APPLICATION_JSON', 
            httpMode: 'POST', 
            requestBody: requestBodyJson, 
            responseHandle: 'NONE', 
            url: jiraUrl + '/rest/api/3/issue', 
            wrapAsMultipart: false
        )

        jiraTicketId = readJSON(text: response.content).key

        notesIndex++
        incidentNotes[notesIndex] = 'A JIRA Ticket has been created:'
        notesIndex++
        incidentNotes[notesIndex] = 'http://aus-bdc-jenkins-cwc2.bmc.com:9090/browse/' + jiraTicketId + '\n\n'

        currentBuild.displayName    = "${BUILD_NUMBER}: Create JIRA Ticket ${jiraTicketId}"
        
/*
        def linkBody            = [:]
        def linkType            = [:]
        def linkInwardIssue     = [:]
        def linkOutwardIssue    = [:]
        def linkComment         = [:]

        linkType.name           = "Blocks"
        linkInwardIssue.key     = jiraTicketId
        linkOutwardIssue.key    = oldJiraTicket
        linkComment.body        = "Abending program - linked to blocked issue ${oldJiraTicket}"

        linkBody.type           = linkType
        linkBody.inwardIssue    = linkInwardIssue
        linkBody.outwardIssue   = linkOutwardIssue
        linkBody.comment        = linkComment

        requestBodyJson         = writeJSON(returnText: true, json: linkBody)

        response = httpRequest(
            customHeaders: [[maskValue: false, name: 'authorization', value: jiraToken]],
            consoleLogResponseBody: true, 
            contentType: 'APPLICATION_JSON', 
            httpMode: 'POST', 
            requestBody: requestBodyJson, 
            responseHandle: 'NONE', 
            url: jiraUrl + '/rest/api/3/issueLink', 
            wrapAsMultipart: false
        )   
*/         
    }

    stage("Send Mail") {

        def emailBody = ""

        incidentNotes.each{
            emailBody = emailBody + it + "\n"
        }

        emailext(
            to: emailRecipient,
            subject: 'Abend Notification: Job ' + abendJobName + ', Program ' + abendProgram, 
            body: emailBody
        )
    }
}
