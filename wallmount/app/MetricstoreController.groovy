import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

import org.json.JSONArray
import org.json.JSONObject
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.events.server.session.AlertSortField
import org.hyperic.hq.galerts.server.session.GalertLogSortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.measurement.server.session.Measurement
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.appdef.shared.AppdefUtil
import org.hyperic.hq.common.Humidor
import org.hyperic.util.PrintfFormat
import java.text.DateFormat

/**
 * Controller to handle metric requests from wallmount metric store.
 * 
 * Requests from wallmount components doesn't ever come directy
 * to HQU backend. Data is always requested through local dojo
 * store which is able to handle caching and provide more wisdom 
 * when data needs to be fetched.
 * 
 * Base URI for this controller is /hqu/wallmount/metricstore/getMetrics.hqu.
 * 
 * getMetrics method is using one parameter named "scope" which defines
 * what kind of data the requester wants to receive.
 */
class MetricstoreController extends BaseJSONController {
    
    /**
     * Local static cache which spans to all instances of
     * this controller.
     * 
     * cache(system) contains timestamp and a map(value)
     * which contains actual cached key/value pairs.
     */
    private static def cache = [system:[time:0,value:[:]]]
    
    /**
     * Static lock to prevent simultaneous requests to
     * update cache at the same time. 
     */
    private static Object systemCacheLock = new Object()
    
	/**
	 * Supress the messages from the log "Invoking method: getMetrics with...."
	 */
	boolean logRequests() {
		false
	}

    /**
     * Returns requested metrics.
     */
    def getMetrics(params) {
        def scope = params.getOne('scope')
        def scopes = scope.split('/')
        JSONArray array = new JSONArray()
        
        if(scopes[0] == 'metric') {
            def scaleId = scopes[1] as int
            def ms
            def tids = scopes[2].split('[|]')
            def tid = tids[0] as int;
            if (tids.size() > 1) {
              def eid = new AppdefEntityID(tids[1])
              if (eid.group) {
                def res = resourceManager.findResource(eid)
                def ress = resourceGroupManager.getMembers(resourceGroupManager.getResourceGroupByResource(res));
                ms = measurementManager.findMeasurements(user, tid, ress.collect { new AppdefEntityID(it.resourceType.appdefType, it.getInstanceId())} as AppdefEntityID[])
              }
            } else {
              ms = [measurementManager.getMeasurement(tid)]
            }
            def lastData = ms.collect { it.lastDataPoint.value }
            if(scaleId == 0) {
                array.put(id: scopes[2], last: lastData.sum()/lastData.size())
            } else {
                def timeScale = scaleId * 60 * 1000.0 as long
                def histData = getHistoricalData(ms, timeScale)
                def data = []
                histData.each{
                    data.push(x:it.timestamp, y:it.value)
                }
                array.put(id: scopes[2], last: lastData.sum()/lastData.size(), serie:data)
            }
        } else if(scopes[0] == 'ravail') {
            def eid = new AppdefEntityID(scopes[1])
            def res = resourceManager.findResource(eid)
            def ress;
            def lress;
            if (eid.group) {
                ress = []
                resourceGroupManager.getMembers(resourceGroupManager.getResourceGroupByResource(res)).each{
                    ress << it.resourceType.appdefType + ":" + it.instanceId
                }
              lress = resourceGroupManager.getMembers(resourceGroupManager.getResourceGroupByResource(res));
            } else {
              ress = [eid.toString()];
              lress = [res];
            }
            def last = lress.collect { availabilityManager.getAvailMeasurement(it).lastDataPoint.value }.min()

			// add the group to the lists of resources so its alerts and escalations will be counted
			// can't add before because looks like the method "lastDataPoint" called above doesn't exist for groups           
			if (eid.group) {
				ress << eid.toString();
				lress << res;
  			}

			//log.info("ravail resources:"+ress)
  
			def resAlertCount = resourcesWithUnfixedAlerts() + groupsWithUnfixedAlerts()
            resAlertCount.keySet().retainAll(ress)
            def alertCount = resAlertCount.values().size() > 0 ? resAlertCount.values().sum() : 0
            
            def resEscCount = resourcesWithRunningEscalation() + groupsWithRunningEscalation()
            resEscCount.keySet().retainAll(ress)
            def escCount = resEscCount.values().size() > 0 ? resEscCount.values().sum() : 0 
            
            array.put(id: scopes[1], last: last, alerts:alertCount, escalations:escCount)
        } else if(scopes[0] == 'tavail') {
            // tavail/1:10100
            // one id means global
            // tavail/1:10607/3:10106
            // two id's means latter within former (e.g. all CPUs within a platform)
        
            def aeid = scopes[-1]
            def within = (scopes[-2] == 'tavail' ? null : scopes[-2])
            
            // get resources
            
            def resources = []
            //log.info("aeid:"+aeid)
            //log.info("within:"+within)
            def proto = resourceManager.findResourcePrototype(new AppdefEntityTypeID(aeid))
            //log.info("proto:"+proto)
            if(within == null) {
                // we're in global mode
                resources = resourceManager.findResourcesOfPrototype(proto, PageInfo.getAll(AlertSortField.RESOURCE, true))
                //log.info("resources1:"+resources)
            } else {
                def eid = new AppdefEntityID(within)
                if(eid.isPlatform() && aeid.startsWith("3:")) {
                    def plat = resourceManager.findResource(eid)
                    //log.info("plat.id1:"+plat.id)
                    //log.info("plat.id2:"+eid.id)
                    def tId = new AppdefEntityTypeID(aeid).id
                    //log.info("tId:"+tId)
                    
                    def serviceValues = serviceManager.getPlatformServices(user, eid.id, tId, PageControl.PAGE_ALL)
                    serviceValues.each{
                        resources << resourceManager.findResource(it.entityId)
                    }
                    //log.info("resources2:"+resources)
                } else if(eid.isPlatform() && aeid.startsWith("2:")) {
                
                    def tId = new AppdefEntityTypeID(aeid).id
                    //log.info("tId:"+tId)
                    def serverValues = serverManager.getServersByPlatform(user, eid.id, tId, true, PageControl.PAGE_ALL)
                    serverValues.each{
                        resources << resourceManager.findResource(it.entityId)
                    }
                    //log.info("resources3:"+resources)
                    
                } else if(eid.isServer() && aeid.startsWith("3:")) {
                    def tId = new AppdefEntityTypeID(aeid).id
                    //log.info("tId:"+tId)
                    def serviceValues = serviceManager.getServicesByServer(user, eid.id,tId ,PageControl.PAGE_ALL)
                    //log.info("serviceValues:"+serviceValues)
                    serviceValues.each{
                        resources << resourceManager.findResource(it.entityId)
                    }
                    //log.info("resources4:"+resources)
                }
            }
            resources.each{
                //log.info("class:"+it.class)
            }

            // avails for resources
            def last = null
            def avails = availabilityManager.getLastAvail(resources,null) 
            avails.each{ key, value ->
                if(last==null) {
                    last = value.value
                } else if(last < value.value) {
                   last = value.value
                }
            }
            
            def ress = []
            resources.each{
                ress << AppdefUtil.newAppdefEntityId(it).toString()
            }

            def resAlertCount = resourcesWithUnfixedAlerts()
            resAlertCount.keySet().retainAll(ress)
            def alertCount = resAlertCount.values().size() > 0 ? resAlertCount.values().sum() : 0
                        
            def resEscCount = resourcesWithRunningEscalation()
            resEscCount.keySet().retainAll(ress)
            def escCount = resEscCount.values().size() > 0 ? resEscCount.values().sum() : 0
            
            
            array.put(id: aeid, last: last, alerts:alertCount, escalations:escCount)
        } else if(scopes[0] == 'system') {
            // need to sync through static lock.
            // only first request within time window
            // will actually update cached values
            synchronized(systemCacheLock) {
                updateSystemStatsCache()
            }
            array.put(id: scopes[-1], last: cache.system.value[scope])
        }
        
        render(inline:"${array}", contentType:'text/json-comment-filtered')
    }
    
    protected void updateSystemStatsCache() {
        // is cache older than 25 sec?
        long time = now()
        if(cache.system.time < time-25000) {
            def stats = getSystemStats()
            cache.system.value = stats
            cache.system.time = time
        }
    }
    
    /**
     * 
     */
    protected getHistoricalData(List<Measurement> ms, long timeScale) {
        def now = System.currentTimeMillis()        
        return dataManager.getHistoricalData(ms, now - timeScale , now, (timeScale / 100) as long, 0, false, PageControl.PAGE_ALL)
    }
    
    /**
     * 
     */
    protected resourcesWithUnfixedAlerts() {
        
        def resAlertCount = [:]
        def severity = AlertSeverity.findByCode(1)
        def alerts = alertHelper.findAlerts(severity, 1232386080468, System.currentTimeMillis(),
                                            false, true, null,
                                            PageInfo.getAll(AlertSortField.RESOURCE, true))
        alerts.each{
            def r = it.alertDefinition.resource
            def eid = r.resourceType.appdefType + ":" + r.instanceId
            if(resAlertCount.containsKey(eid)) {
                (resAlertCount[eid])++
            } else {
                resAlertCount[eid] = 1
            }
        }
        
        resAlertCount
    }
    
    /**
     * 
     */
    protected groupsWithUnfixedAlerts() {
        
		def grpAlertCount = [:]
		def severity = AlertSeverity.findByCode(1)
		def alerts = alertHelper.findGroupAlerts(
				severity, 1232386080468, System.currentTimeMillis(),
                false, true, null,
                PageInfo.getAll(GalertLogSortField.DATE, true))
        alerts.each{
			def eid = "5:" + it.alertDef.group.id

			//log.info("group with unfixed alert: " + eid) 
            
			if(grpAlertCount.containsKey(eid)) {
                (grpAlertCount[eid])++
            } else {
                grpAlertCount[eid] = 1
            }
		}

		grpAlertCount
    }
    
    /**
     * 
     */
    protected resourcesWithRunningEscalation() {
        def resEscCount = [:]
        
        def escStates = escalationManager.getActiveEscalations(10)
        
        escStates.each{
            def aDefId = it.alertDefinitionId
            def aDef = alertDefinitionManager.getByIdAndCheck(user, aDefId)
            def eid = aDef.appdefEntityId.toString()
			
			//log.info("resource with running escalation: " + eid) 
            
            if(resEscCount.containsKey(eid)) {
                (resEscCount[eid])++
            } else {
                resEscCount[eid] = 1
            }

        }
        
        resEscCount
    }

    /**
     * 
     */
    protected groupsWithRunningEscalation() {
        
		def grpEscCount = [:]
		def severity = AlertSeverity.findByCode(1)
		def alerts = alertHelper.findGroupAlerts(
				severity, 1232386080468, System.currentTimeMillis(),
                true, true, null,
                PageInfo.getAll(GalertLogSortField.DATE, true))
        alerts.each{
			def eid = "5:" + it.alertDef.group.id

			//log.info("group with running escalation: " + eid) 
            
			if(grpEscCount.containsKey(eid)) {
                (grpEscCount[eid])++
            } else {
                grpEscCount[eid] = 1
            }
		}

		grpEscCount
    }
    
    /**
     * Returns a map of system statistics.
     */
    private Map getSystemStats() {
        def s = Humidor.instance.sigar
        def loadAvgFmt = new PrintfFormat('%.2f')
        def dateFormat = DateFormat.getDateTimeInstance()
        
        def cpu      = s.cpuPerc
        def sysMem   = s.mem
        def sysSwap  = s.swap
        def pid      = s.pid
        def procFds  = 'unknown'
        def procMem  = s.getProcMem(pid)
        def procCpu  = s.getProcCpu(pid)
        def procTime = s.getProcTime(pid)
        def NA       = 'N/A' //XXX localeBundle?
        def loadAvg1 = NA
        def loadAvg5 = NA
        def loadAvg15 = NA
        def runtime  = Runtime.runtime
            
        try {
            procFds = s.getProcFd(pid).total
        } catch(Exception e) {
        }

        try {
            def loadAvg = s.loadAverage
            loadAvg1  = loadAvgFmt.sprintf(loadAvg[0])
            loadAvg5  = loadAvgFmt.sprintf(loadAvg[1])
            loadAvg15 = loadAvgFmt.sprintf(loadAvg[2])
        } catch(Exception e) {
            //SigarNotImplementedException on Windows
        }

        //e.g. Linux
        def free;
        def used;
        if ((sysMem.free != sysMem.actualFree ||
            (sysMem.used != sysMem.actualUsed))) {
            free = sysMem.actualFree
            used = sysMem.actualUsed
        } else {
            free = sysMem.free
            used = sysMem.used
        }

        [
            'system/syscpu/sysUserCpu':     (int)(cpu.user * 100),
            'system/syscpu/sysSysCpu':      (int)(cpu.sys * 100),
            'system/syscpu/sysNiceCpu':     (int)(cpu.nice * 100),
            'system/syscpu/sysIdleCpu':     (int)(cpu.idle * 100),
            'system/syscpu/sysWaitCpu':     (int)(cpu.wait * 100),
            'system/syscpu/sysPercCpu':     (int)(100 - cpu.idle * 100),
            'system/sysloadavg/loadAvg1':   loadAvg1,
            'system/sysloadavg/loadAvg5':   loadAvg5,
            'system/sysloadavg/loadAvg15':  loadAvg15,
            'system/sysmem/totalMem':       sysMem.total,
            'system/sysmem/usedMem':        used,
            'system/sysmem/freeMem':        free,
            'system/sysswap/totalSwap':     sysSwap.total,
            'system/sysswap/usedSwap':      sysSwap.used,
            'system/sysswap/freeSwap':      sysSwap.free,
            'system/proc/procOpenFds':      procFds,
            'system/proc/procMemSize':      procMem.size,
            'system/proc/procMemRes':       procMem.resident,
            'system/proc/procMemShare':     procMem.share,
            'system/jvm/jvmTotalMem':       runtime.totalMemory(),
            'system/jvm/jvmFreeMem':        runtime.freeMemory(),
            'system/jvm/jvmMaxMem':         runtime.maxMemory(),
            'system/hq/numPlatforms':       resourceHelper.find(count:'platforms'),
            'system/hq/numCpus':            resourceHelper.find(count:'cpus'),
            'system/hq/numAgents':          agentHelper.find(count:'agents'),
            'system/hq/numActiveAgents':    agentHelper.find(count:'activeAgents'),
            'system/hq/numServers':         resourceHelper.find(count:'servers'),
            'system/hq/numServices':        resourceHelper.find(count:'services'),
            'system/hq/numApplications':    resourceHelper.find(count:'applications'),
            'system/hq/numRoles':           resourceHelper.find(count:'roles'),
            'system/hq/numUsers':           resourceHelper.find(count:'users'),
            'system/hq/numAlertDefs':       resourceHelper.find(count:'alertDefs'),
            'system/hq/numResources':       resourceHelper.find(count:'resources'),
            'system/hq/numResourceTypes':   resourceHelper.find(count:'resourceTypes'),
            'system/hq/numGroups':          resourceHelper.find(count:'groups'),
            'system/hq/numEsc':             resourceHelper.find(count:'escalations'),
            'system/hq/numActiveEsc':       resourceHelper.find(count:'activeEscalations'),
            'system/hq/metricsPerMinute':   metricsPerMinute
        ]
    }

    private getMetricsPerMinute() {
        def vals  = measurementManager.findMetricCountSummaries()
        def total = 0.0
        
        for (v in vals) {
            total = total + (float)v.total / (float)v.interval
        }
        (int)total
    }

}