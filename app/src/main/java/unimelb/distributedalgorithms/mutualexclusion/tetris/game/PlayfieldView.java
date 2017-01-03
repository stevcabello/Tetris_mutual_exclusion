package unimelb.distributedalgorithms.mutualexclusion.tetris.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

public class PlayfieldView extends View
{
    private static final SparseArray<Paint> paints;
    private static final Paint border;
    private static final Paint highlight;

    private Playfield playfield;
    private Tetromino block;
    
    private int rows, cols;
	private float left, right, top, bottom;
	private float length;
	private boolean mBlockHilighted;
	
	static
	{
	    // create paints for each Tetromino color
	    paints = new SparseArray<Paint>(Tetromino.colors.length);
	    for (int color: Tetromino.colors)
	    {
	        int faint = color & 0x7FFFFFFF;
	        Paint p = new Paint(), f = new Paint();
	        p.setColor(color);
	        f.setColor(faint);
	        paints.put(color, p);
	        paints.put(faint, f);
	    }
	    border = new Paint();
	    border.setColor(Color.BLACK);
	    highlight = new Paint();
	    highlight.setColor(Color.LTGRAY);
	}
	
	public PlayfieldView(Context context)
	{
		super(context);
	}

	public PlayfieldView(Context context
	        , AttributeSet attrs)
	{
		super(context, attrs);
	}

	public PlayfieldView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	public void setPlayfield(Playfield field)
	{
		playfield = field;
		rows = field.getRows();
		cols = field.getCols();
	}
	
	public Playfield getPlayfield()
	{
	    return playfield;
	}

    public void setBlock(Tetromino block)
    {
        this.block = block;
    }

    public Tetromino getBlock()
    {
        return block;
    }

    public boolean isBlockHighlighted()
    {
        return mBlockHilighted;
    }

    public void setBlockHighlighted(boolean highlightBlock)
    {
        mBlockHilighted = highlightBlock;
    }
    
    // draw the playfield and block on the canvas
	public void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		
		if (playfield != null)
		{
		    // set drawing bounds
	        if (length == 0)
	        {
	            left = getScrollX();
	            top = getScrollY();
	            float width = getWidth();
	            float height = getHeight();
	            float rowLength = height / rows;
	            float colLength = width / cols;

	            
	            if (rowLength <= colLength)
	            {
	                length = rowLength;
	                left += (width - length * cols) / 2;
	                right = left + length * cols - 1;
	                bottom = top + height - 1;
	            }
	            else
	            {
	                length = colLength;
	                top += (height - length * rows) / 2;
	                right = left + width - 1;
	                bottom = top + length * rows - 1;
	            }
	        }
	        
	        // draw border
	        canvas.drawLine(left, top, right, top, highlight);
            canvas.drawLine(left, bottom, right, bottom, highlight);
            canvas.drawLine(left, top, left, bottom, highlight);
            canvas.drawLine(right, top, right, bottom, highlight);

            // draw playfield
            float x0, x1;
            float y0 = top, y1 = y0 + length;
            for (int r = 0; r < rows; r++, y0 = y1, y1 += length)
            {
                x0 = left;
                x1 = x0 + length;
                for (int c = 0; c < cols; c++, x0 = x1, x1 += length)
                {
                    int color = playfield.getColor(r, c);
                    if (color != 0)
                    {
                        canvas.drawRect(x0, y0, x1, y1, paints.get(color));
                        canvas.drawLine(x0, y0, x0, y1, border);
                        canvas.drawLine(x1, y0, x1, y1, border);
                        canvas.drawLine(x0, y0, x1, y0, border);
                        canvas.drawLine(x0, y1, x1, y1, border);
                    }
                }
            }

            // draw block
	        if (block != null)
	        {
    	        Paint paint;
    	        Paint line;
    	        if (mBlockHilighted)
    	        {
    	            paint = paints.get(block.getColor() & 0x7FFFFFFF);
    	            line = highlight;
    	        }
    	        else
    	        {
    	            paint = paints.get(block.getColor());
    	            line = border;
    	        }
    	        
    	        for (Point p: block.getCoords())
    	        {
    	            // check if the part of block is inside bounds
    	            if (p.x >= 0 || p.x < cols || p.y >= 0 || p.y < rows)
    	            {
    	                float x = left + length * p.x, y = top + length * p.y;
    	                canvas.drawRect(x, y, x + length, y + length, paint);
                        canvas.drawLine(x, y, x, y + length, line);
                        canvas.drawLine(x + length, y, x + length, y + length, line);
                        canvas.drawLine(x, y, x + length, y, line);
                        canvas.drawLine(x, y + length, x + length, y + length, line);
    	            }
    	        }
	        }
		}
	}
	
	public void invalidateBlock()
	{
	    invalidate();
	}
	
	public void invalidatePlayfield()
	{
	    invalidate();
	}
}