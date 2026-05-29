/*
Parameters
    cpStream
    cpApplication
    cpAssignment
    cpAssignmentWorkRef
    cpSetId
    cpOperation
    cpSrcLevel
*/

def jiraAssignmentIdField   = "customfield_10082"
def jiraTaskListField       = "customfield_10084"
def jiraIssueId
def jiraIssueCurrentStatus
def jiraIssueNewStatus


def cpTasks                 = []
def cpTaskIndex             = 0

def jiraTasks               = [:]

echo "Parameters received from Code Pipeline Webhook:\n" +
    "  cpStream             : " + cpStream            + "\n" +
    "  cpApplication        : " + cpApplication       + "\n" +
    "  cpAssignment         : " + cpAssignment        + "\n" +
    "  cpAssignmentWorkRef  : " + cpAssignmentWorkRef + "\n" +
    "  cpSetId              : " + cpSetId             + "\n" +
    "  cpOperation          : " + cpOperation         + "\n" +
    "  cpSrcLevel           : " + cpSrcLevel

def jiraStatusOptions   = [
    "To Do": [
        transitionId: "11", 
        name: "To Do"
        ], 
    "In Progress": [
        transitionId: "21", 
        name: "In Progress"
    ],
    "In Review": [
        transitionId: "31", 
        name: "In Review"
    ],
    "Done": [
        transitionId: "41",
        name: "Done"
    ]
]

node{

    jiraIssueId = cpAssignmentWorkRef

    switch(cpOperation) {
        case 'Checkout':
            currentBuild.displayName    = "${BUILD_NUMBER}: Checkout to Assignment ${cpAssignment}, Level ${cpSrcLevel}, Set ${cpSetId}"
            jiraIssueNewStatus          = jiraStatusOptions["In Progress"].name
            break;
        case 'Generate':
            currentBuild.displayName    = "${BUILD_NUMBER}: Generate for Assignment ${cpAssignment}, Level ${cpSrcLevel}, Set ${cpSetId}"
            jiraIssueNewStatus          = jiraStatusOptions["In Progress"].name
            break;
        case 'Promote':
            currentBuild.displayName    = "${BUILD_NUMBER}: Promote for Assignment ${cpAssignment} from Level ${cpSrcLevel}, Set ${cpSetId}"
            jiraIssueNewStatus          = jiraStatusOptions["In Review"].name
            break;
        case 'Regress':
            currentBuild.displayName    = "${BUILD_NUMBER}: Regress for Assignment ${cpAssignment} from Level ${cpSrcLevel}, Set ${cpSetId}"
            jiraIssueNewStatus          = jiraStatusOptions["In Progress"].name
            break;
        default:
            echo "Unsupported operation ${cpOperation}"
            echo "Review your Webhook settings"
            error
            break;
    }    

    stage("Get Info from Code Pipeline") {

        def response = httpRequest(
            acceptType: 'APPLICATION_JSON', 
            consoleLogResponseBody: true, 
            contentType: 'APPLICATION_JSON', 
            validResponseCodes: '100:399',
            customHeaders: 
                [[
                    maskValue: true, 
                    name: 'authorization', 
                    value: cesToken
                ]], 
            url: cesCpUrl + '/ispw/iccga/assignments/' + cpAssignment + '/tasks', 
            wrapAsMultipart: false
        )

        readJSON(text: response.content).tasks.each{

            def jiraTask = [:]
            def taskKey     = it.moduleName + '_' + it.moduleType + '_' + it.level

            jiraTask.name   = it.moduleName
            jiraTask.type   = it.moduleType
            jiraTask.status = it.status
            jiraTask.level  = it.level

            jiraTasks[taskKey] = jiraTask
        }

        jiraTasks = jiraTasks.sort()
/*
        response = httpRequest(
            acceptType: 'APPLICATION_JSON', 
            consoleLogResponseBody: true, 
            contentType: 'APPLICATION_JSON', 
            validResponseCodes: '100:399',
            customHeaders: 
                [[
                    maskValue: true, 
                    name: 'authorization', 
                    value: '665fc9fb-39de-428a-8a67-a3619752873d'
                ]], 
            url: cesUrl + '/ispw/iccga/sets/' + cpSetId + '/tasks', 
            wrapAsMultipart: false
        )

        readJSON(text: response.content).tasks.each{

            def commentTask = [:]
            def taskKey     = it.moduleName + '_' + it.moduleType + '_' + it.level

            commentTask.name   = it.moduleName
            commentTask.type   = it.moduleType
            commentTask.status = it.status
            commentTask.level  = it.level

            commentTasks[taskKey] = commentTask
        }

        commentTasks = commentTasks.sort()

        echo commentTasks.toString()
*/        
    }

    stage("Get JIRA Info") {
        def response = httpRequest(
            acceptType: 'APPLICATION_JSON', 
            customHeaders: 
                [[
                    maskValue: true, 
                    name: 'authorization', 
                    value: jiraToken
                ]], 
            consoleLogResponseBody: true, 
            responseHandle: 'NONE', 
            url: jiraUrl + '/rest/api/3/issue/' + jiraIssueId, 
            wrapAsMultipart: false
        )
        
        def respContent             = readJSON(text: response.content)
        jiraIssueCurrentStatus     = respContent.fields.status.name
    }

    stage("Update JIRA") {

        def textMarksStrong           = []
        textMarksStrong[0]            = [:]
        textMarksStrong[0].type       = "strong"    

        def requestBody             = [:]
        def fields                  = [:]
        def fieldDoc                = [:]
        fieldDoc.type               = "doc"
        fieldDoc.version            = 1
        def fieldDocContent         = []

        fieldDocContent[0]                  = [:]
        fieldDocContent[0].type             = "table"

        def docTable                        = [:]
        def docTableAttrs                   = [:]
        docTableAttrs.isNumberColumnEnabled = false
        docTableAttrs.layout                = "center"
        docTableAttrs.width                 = 900
        docTableAttrs.displayMode           = "default"
        fieldDocContent[0].attrs            = docTableAttrs

        def docTableRows                    = []
        def rowIndex = 0

            docTableRows[rowIndex]              = [:]
            docTableRows[rowIndex].type         = "tableRow"
            
            def tableCells                      = []
            tableCells[0]                       = [:]
            tableCells[0].type                  = "tableCell"
            def cellContent                     = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            def paragraphContent                = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = "Name"
            paragraphContent[0].marks           = textMarksStrong
            
            cellContent[0].content              = paragraphContent
            tableCells[0].content               = cellContent        

            tableCells[1]                       = [:]
            tableCells[1].type                  = "tableCell"
            cellContent                         = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            paragraphContent                    = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = "Type"
            paragraphContent[0].marks           = textMarksStrong
            
            cellContent[0].content              = paragraphContent
            tableCells[1].content               = cellContent        

            tableCells[2]                       = [:]
            tableCells[2].type                  = "tableCell"
            cellContent                         = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            paragraphContent                    = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = "Status"
            paragraphContent[0].marks           = textMarksStrong
            
            cellContent[0].content              = paragraphContent
            tableCells[2].content               = cellContent        

            tableCells[3]                       = [:]
            tableCells[3].type                  = "tableCell"
            cellContent                         = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            paragraphContent                    = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = "Level"
            paragraphContent[0].marks           = textMarksStrong
            
            cellContent[0].content              = paragraphContent
            tableCells[3].content               = cellContent        

            docTableRows[rowIndex].content      = tableCells
            fieldDocContent[0].content          = docTableRows


        jiraTasks.each{

            rowIndex++
            def taskInfo = it.value
            
            docTableRows[rowIndex]              = [:]
            docTableRows[rowIndex].type         = "tableRow"
            
            tableCells                          = []
            tableCells[0]                       = [:]
            tableCells[0].type                  = "tableCell"
            cellContent                         = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            paragraphContent                    = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = taskInfo.name
            
            cellContent[0].content              = paragraphContent
            tableCells[0].content               = cellContent        

            tableCells[1]                       = [:]
            tableCells[1].type                  = "tableCell"
            cellContent                         = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            paragraphContent                    = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = taskInfo.type
            
            cellContent[0].content              = paragraphContent
            tableCells[1].content               = cellContent        

            tableCells[2]                       = [:]
            tableCells[2].type                  = "tableCell"
            cellContent                         = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            paragraphContent                    = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = taskInfo.status
            
            cellContent[0].content              = paragraphContent
            tableCells[2].content               = cellContent        

            tableCells[3]                       = [:]
            tableCells[3].type                  = "tableCell"
            cellContent                         = []
            cellContent[0]                      = [:]
            cellContent[0].type                 = "paragraph"
            
            paragraphContent                    = []
            paragraphContent[0]                 = [:]
            paragraphContent[0].type            = "text"
            paragraphContent[0].text            = taskInfo.level
            
            cellContent[0].content              = paragraphContent
            tableCells[3].content               = cellContent        

            docTableRows[rowIndex].content      = tableCells
            fieldDocContent[0].content          = docTableRows

            if(it.value.level.startsWith("DEV")) {
                jiraIssueNewStatus = jiraStatusOptions["In Progress"].name
            }
        }

        fieldDoc.content         = fieldDocContent

        fields."${jiraTaskListField}" = fieldDoc

        requestBody.fields = fields

        def requestBodyJson = writeJSON(returnText: true, json: requestBody)

        echo "Request Body: "
        echo requestBodyJson

        def response = httpRequest(
            customHeaders: 
                [[
                    maskValue: true, 
                    name: 'authorization', 
                    value: jiraToken
                ]], 
            consoleLogResponseBody: true, 
            contentType: 'APPLICATION_JSON',
            httpMode: 'PUT',
            requestBody: requestBodyJson,
            responseHandle: 'NONE', 
            url: jiraUrl + '/rest/api/3/issue/' + jiraIssueId, 
            wrapAsMultipart: false
        )

        if(jiraIssueCurrentStatus != jiraIssueNewStatus) {

            def transitionBody  = [:]
            def transition      = [:]

            transition.id = jiraStatusOptions[jiraIssueNewStatus].transitionId

            transitionBody.transition = transition

            requestBodyJson = writeJSON(returnText: true, json: transitionBody)

            echo "Request Body:"
            echo requestBodyJson

            def response2 = httpRequest(
            customHeaders: 
                [[
                    maskValue: true, 
                    name: 'authorization', 
                    value: jiraToken
                ]], 
                consoleLogResponseBody: true, 
                contentType: 'APPLICATION_JSON', 
                httpMode: 'POST', 
                requestBody: requestBodyJson, 
                responseHandle: 'NONE', 
                url: jiraUrl + '/rest/api/3/issue/' + jiraIssueId + '/transitions', 
                wrapAsMultipart: false
            )
        }
/*
        def jiraCommentContent = 'Tasks operation ' + cpOperation + ':\n'

        commentTasks.each {
            def taskInfo = it.value
            jiraCommentContent = jiraCommentContent + taskInfo.name + ', ' + taskInfo.type + ', at ' + taskInfo.level + '\n'
        }

        jiraComment(
            body: 'From TicketAutomation:\n' + jiraCommentContent,
            issueKey: jiraIssueId
        ) 
*/        
    }
}
