package unimelb.distributedalgorithms.mutualexclusion.tetris.game;

import android.graphics.Point;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Playfield
{
	private int rows, hidRows, cols;
	private int[][] field;
	private ByteBuffer buffer;
	private ArrayList<Integer> randColors;
	private ArrayList<Integer> randPositions;
	
	public Playfield(int rows, int cols)
	{
	    this(rows, cols, 0);
	}
	
	public Playfield(int rows, int cols, int hiddenRows)
	{
        this.rows = rows;
        this.cols = cols;
        hidRows = hiddenRows;
        buffer = ByteBuffer.allocate(rows * cols * 4);
        
        randColors = new ArrayList<Integer>(Tetromino.colors.length);
        for (int color: Tetromino.colors)
            randColors.add(color);
        
        randPositions = new ArrayList<Integer>(rows);
        for (int i = 0; i < cols; i++)
            randPositions.add(i);
        
        // set all colors to transparent
        field = new int[rows + hidRows][cols];
        for (int[] row: field)
            Arrays.fill(row, 0);
	}
	
	public int getRows()
    {
        return rows;
    }

    public int getCols()
    {
        return cols;
    }

	public int getColor(int row, int col)
	{
	    return field[row + hidRows][col];
	}
	
	// checks whether the block can be inserted into the field
    public boolean canInsert(Tetromino block)
    {
        for (Point p: block.getCoords())
        {
            // check coordinates are within the field
            if (p.x < 0 || p.x >= cols)
                return false;
            if (p.y + hidRows < 0 || p.y >= rows)
                return false;
            if (field[p.y + hidRows][p.x] != 0)
                return false;
        }
        return true;
    }
    
    // inserts the block into the field and return how many rows was cleared
    public int insertBlock(Tetromino block)
    {
        int upperRow = hidRows + rows; 
        int lowerRow = -1;
        int blockColor = block.getColor();

        // set colors in field to same colors as block, find row span of piece
        for (Point p: block.getCoords())
        {
            field[p.y + hidRows][p.x] = blockColor;
            upperRow = Math.min(upperRow, p.y + hidRows);
            lowerRow = Math.max(lowerRow, p.y + hidRows);
        }
        
        // check whether block completed any rows
        int rowsCleared = 0;
        boolean rowFilled;
        for (int r = upperRow; r <= lowerRow; r++)
        {
            rowFilled = true;
            for (int color: field[r])
            {
                if (color == 0)
                {
                    rowFilled = false;
                    break;
                }
            }
            
            if (rowFilled)
            {
                int[] clearedRow = field[r];
                
                // set cleared row's color to transparent
                Arrays.fill(clearedRow, 0);
                
                // shift above rows down 1
                for (int i = r; i > 0; i--)
                    field[i] = field[i - 1];
                
                field[0] = clearedRow;
                rowsCleared++;
            }
        }
        
        return rowsCleared;
    }

    // returns whether there are blocks in hidden rows
    public boolean reachedTop()
    {
        for (int r = 0; r < hidRows; r++)
        {
    	    for (int color: field[r])
    	    {
    			if (color != 0)
    				return true;
    	    }
        }
    	return false;
    }
    
    public int getExportSize()
    {
        return rows * cols * 4;
    }
    
    public byte[] exportField()
    {
        buffer.clear();
        IntBuffer iBuffer = buffer.asIntBuffer();
        for (int r = hidRows; r < hidRows + rows; r++)
           iBuffer.put(field[r]);
        return buffer.array();
    }
    
    public void importField(byte[] bytes)
    {
        buffer.clear();
        buffer.put(bytes);
        buffer.clear();
        IntBuffer iBuffer = buffer.asIntBuffer();
        for (int r = hidRows; r < hidRows + rows; r++)
            iBuffer.get(field[r]);
    }
    
    public void insertRandRow()
    {
        int[] clearRow = field[0];
        Arrays.fill(clearRow, 0);
        Collections.shuffle(randPositions);
        Collections.shuffle(randColors);
        for (int i = 0; i < 5; i++)
            clearRow[randPositions.get(i)] = randColors.get(i);
        
        for (int i = 0; i < rows + hidRows - 1; i++)
            field[i] = field[i+1];
        
        field[rows + hidRows - 1] = clearRow;
    }
}
