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
 * Created on 13/12/2007
 */
package br.com.auster.dware.request.utils;

import java.lang.reflect.Constructor;

/**
 * Esta classe representa um Field (Um campo).
 * A Ideia basica é a de armazenar definições de campos que possuem atributos em comum, tais como:
 * Nome, Posição, tamanho, etc..
 *
 * @author mtengelm
 * @version $Id$
 * @since 13/12/2007
 */
public class FieldDefinition {

	private String				fieldName;

	private int					position, length, index;

	private Constructor	type	= null;

	private boolean			fixed, trimValue = true;

	public FieldDefinition(String name, int position, int length) {
		this.fieldName = name;
		this.position = position;
		this.length = length;
		this.index = 0;
		this.fixed = true;
	}

	public FieldDefinition(int position, int length) {
		this(null, position, length);
	}

	public FieldDefinition(String name, int index) {
		this.fieldName = name;
		this.index = index;
		this.position = this.length = 0;
		this.fixed = false;
	}

	public FieldDefinition(int index) {
		this(null, index);
	}

	public boolean hasType() {
		return (this.type != null);
	}

	public void setType(String className) throws Exception {
		Class clazz = Class.forName(className);
		// type must have a constructor that receives a String as param
		this.type = clazz.getConstructor(new Class[] { String.class });
	}

	public boolean isFixed() {
		return this.fixed;
	}

	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}

	public boolean isTrimValue() {
		return this.trimValue;
	}

	public void setTrimValue(boolean trimValue) {
		this.trimValue = trimValue;
	}

	public String getFieldName() {
		return this.fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public int getPosition() {
		return this.position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getLength() {
		return this.length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getIndex() {
		return this.index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Constructor getType() {
		return this.type;
	}

	public void setType(Constructor type) {
		this.type = type;
	}
}
