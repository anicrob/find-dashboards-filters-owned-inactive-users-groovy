/*
TODO:
(just have to do these steps once and never again)
STEP 1: add the project key where the ticket with the CSV attachments will be created
STEP 2: add the issue type id of the ticket that will be created
STEP 3: add the base URL https://your-domain.atlassian.net

Then run the script in the script console in ScriptRunner! 🏃 

*/
import org.apache.http.entity.ContentType

def createTicket () {
    def projectKey = "SSP" // STEP 1
    def issueTypeId = "10008" // STEP 2
    def createdTicket = post('/rest/api/2/issue')
        .header('Content-Type', 'application/json')
        .body(
                [
                        fields: [
                                summary    : 'Found Dashboards & Filters Owned by Inactive Users',
                                description: "Please review the following.",
                                project    : [
                                        key: projectKey
                                ],
                                issuetype  : [
                                        id: issueTypeId
                                ]
                        ]
                ])
                .asObject(Map)

        assert createdTicket.status >= 200 && createdTicket.status <= 300
        def issueKey = createdTicket.body.key.toString()
        return issueKey
}
  
 def getInactiveFilters () {
        def index = [
            0, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750,
            800, 850, 900, 950, 1000, 1050, 1100, 1150, 1200, 1250, 1300, 1350, 1400,
            1450, 1500, 1550, 1600, 1650, 1700, 1750, 1800, 1850, 1900, 1950, 2000,
            2050, 2100, 2150, 2200, 2250, 2300, 2350, 2400, 2450, 2500, 2550, 2600, 2650, 2700, 2750, 2800, 2850, 2900, 2950
        ];
        def inactiveFilters = []
        index.each { i ->
            Map<String, Object> getFilters = get("/rest/api/3/filter/search?expand=description,owner,jql,viewUrl,searchUrl&maxResults=50&startAt=" + i)
            .header('X-Atlassian-Token', 'no-check')
            .header('Content-Type', 'application/json')
            .header('Accept', 'application/json')
            .asObject(Map).body
            
            getFilters.values.each{ 
                if(it.owner?.active == false) {inactiveFilters << it}
            }
        }
        return inactiveFilters
    }

def createCSV (items, type, issueKey, baseURL) { 
        //create a temp .csv file
        def exportFile = File.createTempFile("tmp", "")
        if(type == "inactiveFilters"){
            try{ 
                exportFile.withPrintWriter { pw ->
                    //create the headers
                    pw.println('Id,Name,Description,Owner,ViewUrl, JQL Query')
        
                    //map the data for each filter
                    items.each { filter ->
                        pw.println("${filter.id},${filter.name},${filter.description ?: ''},${filter.owner.displayName}, ${filter.viewUrl}, ${filter.jql}")
                    }
                }
        
                // post the .csv file created to the Jira issue
                def attachResult = post("/rest/api/3/issue/${issueKey}/attachments")
                    .header('X-Atlassian-Token', 'no-check')
                    .header('Accept', 'application/json')
                    .field("file", exportFile.newInputStream(), ContentType.create("text/csv"), "${type}.csv")
                    .asObject(List)
                
                
                
                if(attachResult.status >= 200 && attachResult.status <= 300) return true
                
                }finally {
                    exportFile.delete()
                }
            } else {
                 try{ 
                exportFile.withPrintWriter { pw ->
                    //create the headers
                    pw.println('Id,Name,Description,Owner,URL')
        
                    //map the data for each dashboard
                    items.each { dashboard ->
                        pw.println("${dashboard.id},${dashboard.name},${dashboard.description ?: ''},${dashboard.owner.displayName}, ${baseURL}/jira/dashboards/${dashboard.id}")
                    }
                }
        
                // post the .csv file created to the Jira issue
                def attachResult = post("/rest/api/3/issue/${issueKey}/attachments")
                    .header('X-Atlassian-Token', 'no-check')
                    .header('Accept', 'application/json')
                    .field("file", exportFile.newInputStream(), ContentType.create("text/csv"), "${type}.csv")
                    .asObject(List)
                
                
                
                if(attachResult.status >= 200 && attachResult.status <= 300) return true
                
                }finally {
                    exportFile.delete()
                }
            }

}

def getInactiveDashboards () {
        def index = [
        0, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750,
        800, 850, 900, 950, 1000, 1050, 1100, 1150, 1200, 1250, 1300, 1350, 1400,
        1450, 1500, 1550, 1600, 1650, 1700, 1750, 1800, 1850, 1900, 1950, 2000,
        2050, 2100, 2150, 2200, 2250, 2300, 2350, 2400, 2450, 2500, 2550, 2600, 2650, 2700, 2750, 2800, 2850, 2900, 2950
        ];
        def inactiveDashboards = [];
         index.each { i ->
            Map<String, Object> getDashboards = get("/rest/api/3/dashboard/search?expand=description,owner,viewUrl&maxResults=50&startAt=" + i)
            .header('X-Atlassian-Token', 'no-check')
            .header('Content-Type', 'application/json')
            .header('Accept', 'application/json')
            .asObject(Map).body
            
            getDashboards?.values?.each{
                if(it.owner?.active == false) {inactiveDashboards << it}
            }
        }
    return inactiveDashboards
}

def script () {
    def baseURL = "https://your-domain.atlassian.net" //STEP 3
    def issueKey = createTicket()
    def filtersForCSV = getInactiveFilters()
    def successfullyCreatedFiltersCSV = createCSV(filtersForCSV, "inactiveFilters", issueKey, baseURL)
    def dashboardsForCSV = getInactiveDashboards()
    def successfullyCreatedDashboardsCSV = createCSV(dashboardsForCSV, "inactiveDashboards", issueKey, baseURL)
        if(successfullyCreatedFiltersCSV && successfullyCreatedDashboardsCSV) {
        println("The CSVs have been successfully attached to ${issueKey} and can be viewed here: ${baseURL}/browse/${issueKey}")
    } else {
        println("There was an error when posting the CSV file to ${issueKey}")
    }
}

script()