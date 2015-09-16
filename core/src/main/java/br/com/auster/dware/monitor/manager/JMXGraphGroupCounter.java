/*
 * Copyright (c) 2004 TTI Tecnologia. All Rights Reserved.
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
 * Created on Jul 7, 2005
 */
package br.com.auster.dware.monitor.manager;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author framos
 * @version $Id: JMXGraphGroupCounter.java 47 2005-07-07 22:31:38Z framos $
 */
public class JMXGraphGroupCounter implements Serializable {


	private AtomicLong[] counters = new AtomicLong[8];
	
	
	public static final int FINISHED_REQUEST_COUNT = 0;
	public static final int ROLLEDBACK_REQUEST_COUNT = 1;
	
	public static final int TOTAL_PROCESSING_TIME = 2;
	public static final int TOTAL_WEIGHT = 3;
	
	public static final int LARGEST_PROCESSED_TIME = 4;
	public static final int LARGEST_PROCESSED_WEIGHT = 5;
	public static final int SMALLEST_PROCESSED_TIME = 6;
	public static final int SMALLEST_PROCESSED_WEIGHT = 7;
	  

	
	
	public JMXGraphGroupCounter() {
		counters[FINISHED_REQUEST_COUNT] = new AtomicLong();
		counters[ROLLEDBACK_REQUEST_COUNT] = new AtomicLong();
		counters[TOTAL_PROCESSING_TIME] = new AtomicLong();
		counters[TOTAL_WEIGHT] = new AtomicLong();

		counters[LARGEST_PROCESSED_TIME] = new AtomicLong();
		counters[LARGEST_PROCESSED_WEIGHT] = new AtomicLong();
		
		counters[SMALLEST_PROCESSED_TIME] = new AtomicLong(Long.MAX_VALUE);
		counters[SMALLEST_PROCESSED_WEIGHT] = new AtomicLong(Long.MAX_VALUE);
	}
	

	/**
	 * Returns the current value of the specified JMX counter
	 * 
	 * @param _counterId
	 * @return
	 */
	public final long getCounter(int _counterId) {
		if ((_counterId < 0) || (_counterId > SMALLEST_PROCESSED_WEIGHT)) {
			throw new IllegalArgumentException("JMX graph counter index exceeds defined limits");
		}
		return counters[_counterId].get();
	}
	
	/**
	 * Adds the defined <code>_value</code> to the JMX counter and returns the previous value. This add-and-get
	 * 	operation is synchronized using Java atomic types. If the value is negative, then the counter will be
	 * 	decremented.
	 * 
	 * @param _counterId
	 * @param _value
	 * @return
	 */
	public long addToCounter(int _counterId, long _value) {
		if ((_counterId < 0) || (_counterId > SMALLEST_PROCESSED_WEIGHT)) {
			throw new IllegalArgumentException("JMX graph counter index exceeds defined limits");
		}
		return counters[_counterId].getAndAdd(_value);
	}
	
	/**
	 * Replaces the current value for the specified counter, and returns its previous value.
	 * 
	 * @param _counterId
	 * @param _value
	 * @return
	 */
	public long setCounter(int _counterId, long _value) {
		if ((_counterId < 0) || (_counterId > SMALLEST_PROCESSED_WEIGHT)) {
			throw new IllegalArgumentException("JMX graph counter index exceeds defined limits");
		}
		return counters[_counterId].getAndSet(_value);
	}
}
