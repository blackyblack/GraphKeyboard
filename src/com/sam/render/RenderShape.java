package com.sam.render;

import android.graphics.Rect;

public class RenderShape {
	public Rect bounds;
	public Integer id;
	public String value;
	public boolean rendered;
	
	public RenderShape(Rect bounds, Integer id, String value)
	{
		this.bounds = new Rect(bounds);
		this.id = id;
		this.rendered = false;
		this.value = new String(value);
	}
	
	public RenderShape(RenderShape shape)
	{
		this.bounds = new Rect(shape.bounds);
		this.id = shape.id;
		this.rendered = shape.rendered;
		this.value = new String(shape.value);
	}
}
