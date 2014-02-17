/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.test;

import com.blackrook.commons.math.Pair;
import com.blackrook.commons.math.Triple;
import com.blackrook.lang.json.annotation.JSONIgnore;

public class JunkObject
{
	public static enum Type
	{
		READ,
		WRITE,
		BUTT;
}
	
	@JSONIgnore
	public int x;
	public int y;
	
	private Type typeA;
	private Type typeB;
	private Object id;
	private Object name;
	private Object enabled;
	private Pair pair;
	private Triple triple;
	private int[] arrayOfInts;
	private float someNumber;
	private Double objectNumber;
	
	public Object getId()
	{
		return id;
}
	public void setId(Object id)
	{
		this.id = id;
}
	public Object getName()
	{
		return name;
}
	public void setName(Object name)
	{
		this.name = name;
}
	public Object isEnabled()
	{
		return enabled;
}
	public void setEnabled(Object enabled)
	{
		this.enabled = enabled;
}
	public Pair getPair()
	{
		return pair;
}
	public void setPair(Pair pair)
	{
		this.pair = pair;
}
	public Triple getTriple()
	{
		return triple;
}
	public void setTriple(Triple triple)
	{
		this.triple = triple;
}
	public int[] getArrayOfInts()
	{
		return arrayOfInts;
}
	public void setArrayOfInts(int[] nums)
	{
		this.arrayOfInts = nums;
}
	public float getSomeNumber()
	{
		return someNumber;
}
	public void setSomeNumber(float someNumber)
	{
		this.someNumber = someNumber;
}
	public Double getObjectNumber()
	{
		return objectNumber;
}
	public void setObjectNumber(Double objectNumber)
	{
		this.objectNumber = objectNumber;
}
	/**
	 * @return the typeA
	 */
	@JSONIgnore
	public Type getTypeA() {
		return typeA;
}
	/**
	 * @param typeA the typeA to set
	 */
	@JSONIgnore
	public void setTypeA(Type typeA) {
		this.typeA = typeA;
}
	/**
	 * @return the typeB
	 */
	@JSONIgnore
	public Type getTypeB() {
		return typeB;
}
	/**
	 * @param typeB the typeB to set
	 */
	@JSONIgnore
	public void setTypeB(Type typeB) {
		this.typeB = typeB;
}
	
	
}
