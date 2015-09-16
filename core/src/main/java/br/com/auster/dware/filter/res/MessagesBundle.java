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
package br.com.auster.dware.filter.res;

import java.util.ListResourceBundle;

/**
 * This class is used by the I18n class for internationalization purposes.
 * 
 * @author version $Id: MessagesBundle.java 282 2007-03-01 17:49:44Z framos $
 */
public final class MessagesBundle extends ListResourceBundle {
    public final Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // ObjectManagerFilter
        {"JAXBIllegal", "Illegal Parameters. Current Element is: {0}"},
		
        // InputFromFile.java
        {"setInputRequest", "Setting the input for request {0} with file {1}"},
        {"problemOpenFile", "Problems opening the input file for request {0}."},
        {"fileDeleted", "File {0} deleted."},
        {"problemDeleting", "Problems deleting file {0}."},
        {"problemClosingFile", "Problems closing file."},

        // OutputToFile.java
        {"setOutputRequest", "Setting the output for request {0} with file {1}."},
        {"problemCreatingFile", "Problems creating the output file for request {0}."},
        {"couldNotMoveFile", "Could not move the file {0} to the new file {1}."},

        // DefaultFilter.java
        {"methodNotSupported", "The method \"{0}\" is not supported by this class."},

        // ThreadedFilter.java
        {"filterThreadReady", "Filter thread ready."},
        {"filterStopped", "Filter thread stopped."},
        {"gotInterruption", "Got an interruption."},

        // PipeConnector.java
        {"problemsPipe", "Problems creating pipe."},
        {"problemClosingWriter", "Problems closing the writer."},
        {"problemClosingReader", "Problems closing the reader."},

        // NIOFilter.java
        {"inputNotSet", "The input must be set before usage (invalid configuration?)."},
        {"outputNotSet", "The output must be set before usage (invalid configuration?)."},

        // XMLReaderFilter.java
        {"defaultXMLReader", "Using a default XMLReader to read the input."},
        {"unsupportedOutputType", "Output type {0} is unsupported by {1}."},
        {"unsupportedInputType", "Input type {0} is unsupported by {1}."},

        // XSLFilter.java
        {"noXSLFiles", "There is no XSL files defined in the configuration for filter {0}."},
        {"usingIncremental", "Using incremental feature in XSL."},
        {"usingXSLTC", "Using XSLT Compiler feature in XSL."},

        // ContentHandlerFilter.java
        {"usingContentHandler", "Using the class {0} as the ContentHandler for the filter {1}."},

        // OffsetDataFromFile.java
        {"problemWriteOutput", "Problems writing to the output."},
        {"readDataNotEnough", "Read data was not enough to be written."},

        // PartialFileRequestFromFileList.java
        {"invalidRequestType", "Invalid request type: {0}"},

        // XMLSplitterFilter.java
        {"chNotNull", "The content handler must not be null."},
        {"chNotFound", "The content handler {0} was not found or defined."},
        
        //CHLimiterPipeFilter
        {"maxSizeExceeded", "The size, {0} , of the request is greater than the configured limit of {1} bytes."},        
                        
        //DataSaverFilter
        {"allFilters.startProcessing", "Starting to process filter ''{0}'' named as ''{1}''."},
        {"allFilters.endProcessing",  "Finished processing filter ''{0}'' named as ''{1}'' in {2}ms."},
        {"allFilters.hasNextFilter", "Next object processor configured...trigerring it."},
        {"allFilters.noNextFilter", "No object processor filter configured next. Finished!"}        
    };
}
