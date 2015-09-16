package br.com.auster.dware.manager.res;

import java.util.ListResourceBundle;

/**
 * This class is used by the I18n class for internationalization purposes.
 *
 * @version $Id: MessagesBundle.java 334 2007-12-04 16:27:08Z framos $
 */
public class MessagesBundle extends ListResourceBundle {
    public Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // GraphGroup.java
        {"graphGroupReady", "Graph Group ready for requests!"},
        {"processingLastRequests", "Processing last requests before exiting..."},
        {"graphGroupDown", "Graph Group down."},
        {"graphGroupGoingDown", "The Graph Group \"{0}\" is going down."},
        {"graphMedException", "The mediator can't be null."},
        {"graphFailed", "Fatal error in GraphGroup. The Graph did not handle the exception!"},

        // LocalGraphGroup.java
        {"maxGraphsNumber", "Setting the maximum number of graphs for group \"{0}\" to {1}."},
        {"settingGraphs", "Setting the graph configuration for Graph Group \"{0}\"."},
        {"settingFilters", "Setting the filter \"{0}\" configuration for Graph Group \"{1}\"."},
        {"graphDoesNotExist", "The graph \"{0}\" does not exist in the graph list for Graph Group \"{1}\"!"},
        {"listenerConfigured", "Listener \"{1}\" configured for group \"{0}\"."},
        {"listenerConfigError", "Error while configuring Listener \"{1}\" for group \"{0}\"!"},
        {"listenerFinishError", "Error while alerting Listener for group \"{0}\" of a finished request!"},

        // GraphManager.java
        {"configuringManager", "Configuring the Graph Manager..."},
        {"managerConfigured", "Graph Manager configured!"},
        {"creatingLocalGraphGroup", "Creating the Local Graph Group \"{0}\"."},
        {"groupAlreadyExists", "The group \"{0}\" already exists in the graph manager."},
        {"groupDoesNotExist", "The group \"{0}\" does not exist in the graph manager."},
        {"configuringGroup", "Configuring Graph Group \"{0}\"."},
        {"cantEnqueueManagerDown", "Can't enqueue request {0}. This graph manager is down."},
        {"managerGoingDown", "Shutting down the Graph Manager!"},
        {"gotInterruption", "Got interruption."},
        {"managerDown", "Graph Manager down."},
        {"queueListenersNotConfigured", "Error while configuring queue-empty listeners - ignoring all listeners."},
        {"queueListenerConfigured", "Added and configured QueueEmptyListener \"{0}\"."},
        {"checkpointNotConfigured", "The checkpoint was not configured."},
        {"checkpointCreationError", "The checkpoint was not succesfully created."},

        // ReqForwarder
        {"reqAlreadyLoaded", "The request \"{0}\" is already loaded."},

        // remote
        {"creatingRemoteGraphGroup", "Creating remote graph group."},
        {"remoteConfigErr", "Error in remote graph configuration."},
        {"remoteProcessErr", "Error processing remote request."},
        {"remoteShutdownErr", "Error in shutdown of remote graph group."},
        {"remoteFilterCfgErr", "Error configuring remote filters."},
        {"remoteReplyCommitErr", "Error in confirm the commit of a request."},
        {"remoteManagerServiceErr", "Error configuring jndi name for GraphManager. Remote graph groups will not be able to connect."},
        {"remoteManagerConfigErr", "Graph manager was not configured."},
        {"remoteManagerRegisterErr", "Error registering remote graph group."},
        {"remoteTerminateErr", "Error terminating remote graph group."},
        {"remoteFinalizeErr", "Error finalizing remote graph group."},
        {"remoteKillErr", "Error killing remote graph group."},
        {"remoteXMLConfigErr", "Error configuring remote graph group. XML format shoud be incorrect."},
        {"remoteReqUnexpected", "Message received from some request that finishes and was NOT processed by this graph group."},
        {"remoteConnErr", "Remote graph group could not connect because graph manager is shutting down."},
        {"remoteSrvDownErr", "Shutting down remote client graph group because server is down."}
    };
}
