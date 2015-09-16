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
package br.com.auster.dware.res;

import java.util.ListResourceBundle;

/**
 * @version $Id: MessagesBundle.java 336 2007-12-26 13:14:17Z mtengelm $
 */
public class MessagesBundle extends ListResourceBundle {
	public Object[][] getContents() {
		return contents;
	}

	static final Object[][]	contents	= {
			// Bootstrap.java
			{ "file.save.error", "Problems saving requests on file [ {0} ]." },
			{ "file.restore.error", "Problems restoring requests from file [ {0} ]." },
			{ "index.only", "Index only option choosen. We got {0} requests." },
			{ "init.error", "Bootstrap have not been initialized." },
			{ "sendingRequests", "Sending {0} requests to process." },
			{ "incompatible.options", "Option {0} and option {1} are not compatible." },
			// DataAware.java
			{ "listenerReady", "StartupListener [ {0} ] configured and ready!" },
			{ "managerNotConfigured", "Graph Manager was not configured!" },
			{ "resourceMissing", "A needed resource is missing.Resource name is {0}" } };
}
