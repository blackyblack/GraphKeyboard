package com.sam.render;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.util.Log;

/*
 * This class "renders" shapes which is looking for most priority shape,
 * set it as a base shape and placing around other shapes.
 * Result is returned as a list of rendered shapes.
 */

public class RenderEngine {
	private Rect screenBounds;
	private List<RenderShape> shapes;
	private List<RenderShape> renderedShapes;
	
	public RenderEngine()
	{	
		screenBounds = new Rect();
		shapes = new ArrayList<RenderShape>(30);
		renderedShapes = new ArrayList<RenderShape>(30);
	}
	
	public void setScreen(Rect screenBounds)
	{
		this.screenBounds = new Rect(screenBounds);
	}
	
	//ask RenderEngine for bounds of a single key based on screen bounds
	//and total count of keys
	public int getDesiredSize(int numKeys)
	{
		if(screenBounds == null)
			return 0;
		
	    int defaultSize = 110;
	    int defaultKeys = 29;
	    
	    double a = (Math.sqrt(screenBounds.width() * screenBounds.height() * 1.0)) / numKeys;
	    double b = (defaultSize * 1.0) / ((Math.sqrt(762 * 480 * 1.0)) / defaultKeys);
	    
	    return (int)(a * b);
	}
	
	//erase all inner shapes information including fixed shapes
	public void clear()
	{
		shapes.clear();
		renderedShapes.clear();
	}
	
	//add shape to renderer
	//"priority" is a sort order of processing shapes. greater priorities
	//will be processed first - so will be placed closer to the base shape
	//set "rendered" to true to fix shape position on the screen
	public void addShape(RenderShape shape)
	{
		shapes.add(shape);
	}
	
	//get rendered shape by id
	//return null if nothing found
	public List<RenderShape> getShapes()
	{		
		return renderedShapes;
	}
	
	//execute render procedure
	public void start()
	{
		RenderShape baseShape = null;
		RenderShape foundShape = null;
		boolean result;
		
		//Log.v("render", "render start");
		
		List<RenderShape> excludedShapes = new ArrayList<RenderShape>();
		renderedShapes.clear();
		
		//make a copy of already rendered shapes into helper list
		for(RenderShape s : shapes)
		{
			if(s.rendered)
				renderedShapes.add(s);
		}
		
		//initial base shape
		baseShape = searchBaseShape(excludedShapes);
		
		//render all shapes
		while(true)
		{			
			//try to find shape to render
			foundShape = searchBestShape();
		
			//conditions to stop rendering
			if(foundShape == null)
			{
				//Log.v("render", "render done");
				return;
			}
			
			//Log.v("render", "foundShape is " + foundShape.value);
		
			while(true)
			{
				//select base shape
				baseShape = searchBaseShape(excludedShapes);
						
				//conditions to stop rendering
				if(baseShape == null)
				{
					//Log.v("render", "render done");
					return;
				}
				
				//Log.v("render", "baseShape is " + baseShape.value);
		
				//and render it
				result = tryRender(excludedShapes, foundShape, baseShape);
			
				if(result)
				{
					//Log.v("render", "result baseShape is " + baseShape.value);
					break;
				}
			}
		}
	}
	
	//single step of rendering
	//return false if need to search for better base shape
	private boolean tryRender(List<RenderShape> excludedShapes, 
			RenderShape shape, RenderShape baseShape)
	{
		boolean result;
				
		//Log.v("render", "base shape " + baseShape.bounds.toString() + " id = " + (char)baseShape.id);
		//Log.v("render", "best shape " + shape.bounds.toString() + " id = " + (char)shape.id);
				
		//try to place shape near base
		result = placeShapeNearBase(shape, baseShape);
				
		//if no place around base shape - look for better one
		if(!result)
		{
			excludedShapes.add(baseShape);
			return false;
		}
		
		return true;
	}
	
	//search in shapes for a base shape to start drawing around it
	//1. searches into already rendered shapes for a maximum priority (least value) shape
	//2. if found shape is not in excludedShapes - select it as base
	//3. if no rendered shapes found - look into unrendered shapes for a maximum
	//   priority shape
	//4. if shape found - place it in the center of the screen, set as rendered
	//   and select as base
	private RenderShape searchBaseShape(List<RenderShape> excludedShapes)
	{
		RenderShape foundShape = null;
		
		//if we have rendered shapes - look for max priority
		for (RenderShape shape1 : renderedShapes) 
		{						
			//skip already filled shapes
			if((excludedShapes != null) && (excludedShapes.contains(shape1)))
				continue;
			
			return shape1;
		}

		// if no good fixed shapes found - start looking at other shapes
		for (RenderShape shape1 : shapes) 
		{			
			if(shape1.rendered)
				continue;
			
			foundShape = shape1;
			break;
		}
				
		//no shapes in list!
		if(foundShape == null)
		{
			Log.v("render", "no shapes to render");
			return null;
		}
		
		//move first shape to lower right corner
		int width = foundShape.bounds.width();
		
		foundShape.bounds.bottom = screenBounds.bottom;
		foundShape.bounds.right = screenBounds.right;
		foundShape.bounds.left = foundShape.bounds.right - width;
		foundShape.bounds.top = foundShape.bounds.bottom - width;

		foundShape.rendered = true;
		
		renderedShapes.add(foundShape);
		
		return foundShape;
	}
	
	//look for not rendered shape with max priority
	private RenderShape searchBestShape()
	{		
		for (RenderShape shape1 : shapes) 
		{					
			if(!shape1.rendered)
			{
				return shape1;
			}
		}

		return null;
	}
	
	//try to place shape near baseShape
	private boolean placeShapeNearBase(RenderShape shape, RenderShape baseShape)
	{
		int i;
		boolean found;
		int baseX = baseShape.bounds.centerX();
		int baseY = baseShape.bounds.centerY();
		//int baseRadius = baseShape.bounds.height() / 2;
		int baseRadius = (int)Math.sqrt(
				Math.pow(baseShape.bounds.height() / 2, 2) + 
				Math.pow(baseShape.bounds.width() / 2, 2)) + 5;
		
		int shapeRadius = shape.bounds.height() / 2;

		//will offset our shape from the center of base shape
		centerShapes(shape, baseShape);
		
		found = false;
		
		//цикл с интервалом в 15 градусов
		for(i = 180; i > -180; i -= 15)
		{
			//calc center of the desired placement
			int x = (int)((baseRadius + shapeRadius) * Math.cos((2 * Math.PI / 360) * i));
			int y = -(int)((baseRadius + shapeRadius) * Math.sin((2 * Math.PI / 360) * i));
			
			if((baseY - shapeRadius + y) < screenBounds.top)
			{
				//Log.v("render", "screen top");
				continue;
			}
			if((baseX - shapeRadius + x)  < screenBounds.left)
			{
				//Log.v("render", "screen left");
				continue;
			}
			if((baseY + shapeRadius + y)  > screenBounds.bottom)
			{
				//Log.v("render", "screen bottom");
				continue;
			}
			if((baseX + shapeRadius + x) > screenBounds.right)
			{
				//Log.v("render", "screen right");
				continue;
			}
			
			//now should check intersection with rendered shapes
			
			shape.bounds.offset(x, y);
			
			if(checkIntersections(shape))
			{
				centerShapes(shape, baseShape);
				continue;
			}
			
			found = true;
			break;
		}
		
		//message upper level that base shape does not suite
		if(!found)
			return false;
		
		//place on screen
		shape.rendered = true;		
		renderedShapes.add(shape);
		
		return true;
	}
	
	//helper to center shapes
	private void centerShapes(RenderShape what, RenderShape where)
	{
		int baseX = where.bounds.centerX();
		int baseY = where.bounds.centerY();
		
		int width = what.bounds.width();
		int height = what.bounds.height();
		
		what.bounds.left = baseX - (what.bounds.width() / 2);
		what.bounds.top = baseY - (what.bounds.height() / 2);
		what.bounds.right = what.bounds.left + width;
		what.bounds.bottom = what.bounds.top + height;
	}
	
	//search for intersections with already rendered shapes
	//return false if no intersections
	private boolean checkIntersections(RenderShape shape)
	{
		if(renderedShapes == null)
			return false;
		
		int left = shape.bounds.left;
		int top = shape.bounds.top;
		int right = shape.bounds.right;
		int bottom = shape.bounds.bottom;
		
		for (RenderShape shape1 : renderedShapes) 
		{			
			if(shape1.bounds.intersects(left, top, right, bottom))
			{
				return true;
			}
			
			if(shape1.bounds.contains(shape.bounds))
			{
				return true;
			}
			
			if(shape.bounds.contains(shape1.bounds))
			{
				return true;
			}
		}
		
		return false;
	}
}
