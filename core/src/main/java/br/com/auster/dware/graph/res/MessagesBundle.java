/*
 * Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Created on Apr 8, 2005
 */
package br.com.auster.dware.graph.res;

import java.util.ListResourceBundle;

/**
 * This class is used by the I18n class for internationalization purposes.
 * 
 * @version $Id: MessagesBundle.java 306 2007-09-05 00:13:01Z rbarone $
 */
public class MessagesBundle extends ListResourceBundle {
    public Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // Graph.java
        {"filterListEmpty", "The graph configuration must have 'filter' elements."},
        {"addFilter", "Adding the filter {0}..."},
        {"noConnectionFilter", "No connection out from filter {0}."},
        {"filterAlreadyAdded", "Trying to add the filter \"{0}\" that is already in the list (duplicated filter name in configuration?)."},
        {"makingConnection", "Creating the edge [ {0} -> {1} ]..."},
        {"connectionFilterFailed", "One or both of the filters \"{0}\" or \"{1}\" were not added to the filter graph. Check your configuration."},
        {"invalidConnectorClassName", "Could not instantiate the connector \"{0}\". Default connectors will be used instead."},
        {"processingRequest", "Starting to process request {0} at graph \"{1}\"."},
        {"waitingRequest", "Waiting for the end of the processing of the request {0} at graph \"{1}\"."},
        {"rollingbackRequest", "Rolling back processing for request {0} at graph \"{1}\"."},
        {"commitingRequest", "Commiting processing for request {0} at graph \"{1}\"."},
        {"finishListenerProblem", "Exception received while calling the finish listener for request {0} at graph \"{1}\"."},
        {"totalProcTime", "Total processing time for {0}: {1}"},
        {"noConfig", "This graph is not configured. It may have already been shutted down. Configure it before trying to process any request."},
        {"threadAlive", "The thread \"{0}\" is already alive. Not starting it again."},
        {"noSuchFilter", "The filter \"{0}\" does not exist in graph \"{1}\"."},
        {"configuringGraph", "Configuring the graph \"{0}\"..."},
        {"graphConfigured", "Finished configuring graph \"{0}\"."},
        {"gotInterruption", "Got an interruption."},
        {"graphGoingDown", "Started shutdown process for graph \"{0}\"."},
        {"graphDown", "Shutdown process for graph \"{0}\" finished."},
        {"listenerConfigured", "Listener \"{1}\" configured for graph \"{0}\"."},
        {"listenerConfigError", "Error while configuring Listener \"{1}\" for graph \"{0}\"!"},
        {"listenerFinishError", "Error while alerting Listener for graph \"{0}\" of a finished request!"},
        {"commitError", "Error while commiting request {0} for graph \"{1}\"."},
        {"rollbackError", "Error while rolling back request {0} for graph \"{1}\"."},
        {"counterUpdateError", "Error while updating Graph \"{1}\" counters."},
        {"shouldNotBeDead", "A threaded filter was found not alive when graph \"{0}\" was requested to start processing."},
        {"threadedFilterDown", "The threaded filter \"{1}\" of graph \"{0}\" is not alive - forcing reconfiguration of all filters."},
        {"invalidTimeout", "Invalid timeout configuration \"{0}\": must be an integer greater than zero."},
        {"idleTimeout", "Graph \"{0}\" idle timeout has been reached - shutting down graph..."},

        // DefaultFilter.java
        {"methodNotSupported", "The method \"{0}\" is not supported by this class."},

        // ThreadedFilter.java
        {"gotInterruption", "Got an interruption."},
        {"filterThreadReady", "Filter thread ready."},
        {"filterStopped", "Filter thread stopped."},
        {"waitingThread", "Wating for the threaded filter {0} to finish."},

        // Edge.java
        {"noConnectorsAvailable", "Could not find any connectors available."},
        {"noConnectorAcceptable", "Could not find an acceptable connector for the edge [ {0} -> {1} ]"}
    };
}
