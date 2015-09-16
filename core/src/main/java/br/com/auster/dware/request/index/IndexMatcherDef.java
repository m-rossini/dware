/*
 * Copyright (c) 2004-2007 Auster Solutions. All Rights Reserved.
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
 * Created on 28/11/2007
 */
package br.com.auster.dware.request.index;

import org.apache.commons.lang.StringUtils;

/**
 * TODO What this class is responsible for
 * 
 * @author mtengelm
 * @version $Id$
 * @since JDK1.4
 */
public class IndexMatcherDef {

	public static final String	ALIGN_LEFT	= "left";
	public static final String	ALIGN_RIGHT	= "right";

	private int									start;
	private int									size;
	private String							align;
	private String							fill;
	private boolean							trim;

	/**
	 * Creates a new instance of the class <code>IndexMatcher</code>.
	 * 
	 * @param useTrim
	 * @param useFill
	 * @param useAlign
	 * @param useSize
	 * @param useStart
	 */
	public IndexMatcherDef(int useStart, int useSize, String useAlign, String useFill,
			boolean useTrim) {
		this.start = useStart;
		this.size = useSize;
		this.align = useAlign;
		this.fill = useFill;
		this.trim = useTrim;
	}

	/**
	 * Return the value of a attribute <code>start</code>.
	 * 
	 * @return return the value of <code>start</code>.
	 */
	public int getStart() {
		return this.start;
	}

	/**
	 * Return the value of a attribute <code>size</code>.
	 * 
	 * @return return the value of <code>size</code>.
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Return the value of a attribute <code>align</code>.
	 * 
	 * @return return the value of <code>align</code>.
	 */
	public String getAlign() {
		return this.align;
	}

	/**
	 * Return the value of a attribute <code>fill</code>.
	 * 
	 * @return return the value of <code>fill</code>.
	 */
	public String getFill() {
		return this.fill;
	}

	/**
	 * Return the value of a attribute <code>trim</code>.
	 * 
	 * @return return the value of <code>trim</code>.
	 */
	public boolean isTrim() {
		return this.trim;
	}

	public static String handleSequence(String seq, IndexMatcherDef imd) {
		int size = (imd.getSize() == Integer.MAX_VALUE) ? seq.length() : imd.getSize();
		String results = StringUtils.substring(seq, imd.getStart(), imd.getStart()
				+ size);
		if (null != imd.getAlign()) {
			if (ALIGN_LEFT.equals( imd.getAlign() )) {
				results = StringUtils.rightPad(results, size, imd.getFill());
			} else if (ALIGN_RIGHT.equals( imd.getAlign() )) {
				results = StringUtils.leftPad(results, size, imd.getFill());
			}
		}
		return (imd.isTrim()) ? StringUtils.trimToEmpty(results) : results;
	}
}
