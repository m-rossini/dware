/*
* Copyright (c) 2004-2005 Auster Solutions. All Rights Reserved.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
* THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
* OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
* EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
* Created on 08/07/2005
*/
package br.com.auster.dware.graph;

import java.util.concurrent.atomic.AtomicLongArray;

public interface FilterMonitor extends Filter {
   
   public int NUMBER_OF_COUNTERS = 5;
   /***
    * Optional Counter. Must be present, althrougth can be any value. Minus 1 (-1) assume not initialized or invalid
    * If not equal -1 represents the number of processed requests by the filter
    */
   public final int PROCESSED_REQUESTS = 0;

   /***
    * Optional Counter. Must be present, althrougth can be any value. Minus 1 (-1) assume not initialized or invalid
    * If not equal -1 represents the total processing time of the filter
    */
   public final int TOTAL_PROCESSING_TIME = 1;
   
   /***
    * Optional Counter. Must be present, althrougth can be any value. Minus 1 (-1) assume not initialized or invalid
    * If not equal -1 represents the time took be the request that had the highest processing time
    */
   public final int HIGHEST_PROCESSING_TIME = 2;
   
   /***
    * Optional Counter. Must be present, althrougth can be any value. Minus 1 (-1) assume not initialized or invalid
    * If not equal -1 represents the time took be the request that had the lowest processing time
    */   
   public final int LOWEST_PROCESSING_TIME = 3;
   
   /***
    * Optional Counter. Must be present, althrougth can be any value. Minus 1 (-1) assume not initialized or invalid
    * If not equal -1 it is filter dependent in its context, see specfic filters implementation for its meaning.
    */
   public final int PROCESSED_INFO_CHUNCKS = 4;
   
   /***
    * Gets an AtomicLongArray of counters, where each position represents a specific counter.
    * @return
    */
   public AtomicLongArray getCounters();
   public void resetCounters();
}
 