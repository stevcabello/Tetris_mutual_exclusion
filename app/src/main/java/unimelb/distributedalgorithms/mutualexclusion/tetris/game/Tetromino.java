package unimelb.distributedalgorithms.mutualexclusion.tetris.game;

import android.graphics.Color;
import android.graphics.Point;

public class Tetromino
{
    public static final int colors[];
    public static final Tetromino blockTypes[];

    static
    {
        colors = new int[] {Color.CYAN, Color.BLUE, Color.rgb(255, 127, 0), Color.YELLOW, Color.GREEN, Color.rgb(127, 0, 127), Color.RED};

        blockTypes = new Tetromino[7];
        blockTypes[0] = new Tetromino(colors[0], true, new Point[] {new Point(-2, 1), new Point(-1, 1), new Point(1, 1), new Point(2, 1)});
        blockTypes[1] = new Tetromino(colors[1], false, new Point[] {new Point(-1, -1), new Point(-1, 0), new Point(0, 0), new Point(1, 0)});
        blockTypes[2] = new Tetromino(colors[2], false, new Point[] {new Point(1, -1), new Point(-1, 0), new Point(0, 0), new Point(1, 0)});
        blockTypes[3] = new Tetromino(colors[3], true, new Point[] {new Point(-1, -1), new Point(1, -1), new Point(-1, 1), new Point(1, 1)});
        blockTypes[4] = new Tetromino(colors[4], false, new Point[] {new Point(0, -1), new Point(1, -1), new Point(-1, 0), new Point(0, 0)});
        blockTypes[5] = new Tetromino(colors[5], false, new Point[] {new Point(0, -1), new Point(-1, 0), new Point(0, 0), new Point(1, 0)});
        blockTypes[6] = new Tetromino(colors[6], false, new Point[] {new Point(-1, -1), new Point(0, -1), new Point(0, 0), new Point(1, 0)});
    }

	private int color;
	private Point[] relativePts;
	private boolean cornerCentered;

	private Point center;
    private Point realPts[];

	
	// create empty Tetromino
	public Tetromino()
	{
	    this(0, false, new Point[]{new Point(), new Point(), new Point(), new Point()});
	}


	// used to create template of Tetrominos
    public Tetromino(int color, boolean cornerCentered, Point coords[])
    {
        this.color = color;
        center = new Point();
        relativePts = coords;
        this.cornerCentered = cornerCentered;
        realPts = new Point[coords.length];
        for (int i = 0; i < realPts.length; i++)
            realPts[i] = new Point();
    }




    /**
     * Creates random block
     * @param randIdx
     */
    public void copyRandBlock(int randIdx)
    {
        Tetromino rand =  (Tetromino)blockTypes[randIdx];
        color = rand.color;
        cornerCentered = rand.cornerCentered;
        for (int i = 0; i < relativePts.length; i++)
        {
            Point tPt = rand.relativePts[i];
            relativePts[i].set(tPt.x, tPt.y);
        }


    }




	public void setCenter(Point p)
	{
		center = p;
	}
	
	public Point getCenter()
	{
		return center;
	}
	
	public int getColor()
	{
		return color;
	}

	public Point[] getCoords()
	{
	    // calculate coordinates
        if (!cornerCentered)
        {
            for (int i = 0; i < realPts.length; i++)
                realPts[i].set(relativePts[i].x + center.x, relativePts[i].y + center.y);
        }
        else
        {
            for (int i = 0; i < realPts.length; i++)
            {
                if (relativePts[i].x > 0)
                    realPts[i].x = relativePts[i].x + center.x;
                else
                    realPts[i].x = relativePts[i].x + center.x + 1;
                
                if (relativePts[i].y < 0)
                    realPts[i].y = relativePts[i].y + center.y;
                else
                    realPts[i].y = relativePts[i].y + center.y - 1;
            }
        }

        return realPts;
	}
	
	// Rotate the block by 90 degree clockwise
	public void rotateCW()
	{
	    for (Point p: relativePts)
	        p.set(-p.y, p.x);
	}
}
