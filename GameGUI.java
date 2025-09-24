import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Point;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import javax.swing.Timer;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.Random;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;

/**
 * A Game board on which to place and move players.
 * 
 * @author PLTW
 * @version 1.0
 */
public class GameGUI extends JComponent
{
  static final long serialVersionUID = 141L; // problem 1.4.1

  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int START_LOC_X = 15;
  private static final int START_LOC_Y = 15;
  
  // initial placement of player
  int x = START_LOC_X; 
  int y = START_LOC_Y;

  private Image bgImage;
  private Image player;
  private Point playerLoc;
  private int playerSteps;

  // walls, prizes, traps
  private int totalWalls;
  private Rectangle[] walls; 
  private Image prizeImage;
  private int totalPrizes;
  private Rectangle[] prizes;
  private int totalTraps;
  private Rectangle[] traps;

  private int prizeVal = 10;
  private int trapVal = 5;
  private int endVal = 10;
  private int offGridVal = 5; 
  private int hitWallVal = 5; 

  private JFrame frame;

  private int score = 10; 
  private int movesLeft = 20; 
  private boolean timerExpired = false;

  // Fake coin trap that looks like a prize
  private Rectangle fakeCoin; 
  private boolean fakeCoinActive = true;

  // 3 Scan ability to detect fake coin ahead
  private int scanCount = 3; 

  // Stats panel components
  private JPanel statsPanel;
  private JLabel scoreLabel;
  private JLabel movesLabel;
  private JLabel timerLabel;
  private JLabel scansLabel;
  private Timer swingTimer;
  private int timeLeft = 15;

  /**
   * Constructor for the GameGUI class.
   * Creates a frame with a background image and a player that will move around the board.
   */
  public GameGUI()
  {
    
    try {
      bgImage = ImageIO.read(new File("chap1problem_escaperoom-main/grid.png"));      
    } catch (Exception e) {
      System.err.println("Could not open file grid.png");
    }      
    try {
      prizeImage = ImageIO.read(new File("chap1problem_escaperoom-main/coin.png"));      
    } catch (Exception e) {
      System.err.println("Could not open file coin.png");
    }
    try {
      player = ImageIO.read(new File("chap1problem_escaperoom-main/player.png"));      
    } catch (Exception e) {
     System.err.println("Could not open file player.png");
    }
    playerLoc = new Point(x,y);

    // create the game frame
    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH + 150, HEIGHT); 
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(this, BorderLayout.CENTER);
    frame.setResizable(false);
    statsPanel = new JPanel();
    statsPanel.setLayout(new java.awt.GridLayout(4, 1));
    scoreLabel = new JLabel("Score: " + score, SwingConstants.CENTER);
    movesLabel = new JLabel("Moves left: " + movesLeft, SwingConstants.CENTER);
    timerLabel = new JLabel("Time left: " + timeLeft + "s", SwingConstants.CENTER);
    scansLabel = new JLabel("Scans left: " + scanCount, SwingConstants.CENTER);
    statsPanel.add(scoreLabel);
    statsPanel.add(movesLabel);
    statsPanel.add(timerLabel);
    statsPanel.add(scansLabel);
    frame.add(statsPanel, BorderLayout.EAST);
    frame.setVisible(true);

    // set default config
    totalWalls = 20;
    totalPrizes = 3;
    totalTraps = 5;

    // Start the timer
    swingTimer = new Timer(1000, evt -> {
      timeLeft--;
      timerLabel.setText("Time left: " + timeLeft + "s");
      if (timeLeft <= 0) {
        timerExpired = true;
        JOptionPane.showMessageDialog(frame, "Game Over! Time's up.");
        endGame();
        swingTimer.stop();
      }
    });
    swingTimer.start();

    // e for pickup, WASD for jump, q for detrap, f for scan
    frame.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (score <= 0 || movesLeft <= 0 || timerExpired) return;
        int key = e.getKeyCode();
        boolean moved = false;
        int moveResult = 0;
        if (key == KeyEvent.VK_RIGHT) {
          moveResult = movePlayer(SPACE_SIZE, 0);
          moved = true;
        } else if (key == KeyEvent.VK_LEFT) {
          moveResult = movePlayer(-SPACE_SIZE, 0);
          moved = true;
        } else if (key == KeyEvent.VK_UP) {
          moveResult = movePlayer(0, -SPACE_SIZE);
          moved = true;
        } else if (key == KeyEvent.VK_DOWN) {
          moveResult = movePlayer(0, SPACE_SIZE);
          moved = true;
        }
        else if (key == KeyEvent.VK_W) {
          moveResult = movePlayer(0, -2 * SPACE_SIZE);
          moved = true;
        } else if (key == KeyEvent.VK_A) {
          moveResult = movePlayer(-2 * SPACE_SIZE, 0);
          moved = true;
        } else if (key == KeyEvent.VK_S) {
          moveResult = movePlayer(0, 2 * SPACE_SIZE);
          moved = true;
        } else if (key == KeyEvent.VK_D) {
          moveResult = movePlayer(2 * SPACE_SIZE, 0);
          moved = true;
        }
        else if (key == KeyEvent.VK_E) {
          if (isOnFakeCoin() && fakeCoinActive) {
            score -= 5;
            JOptionPane.showMessageDialog(frame, "It's a trap! Score: " + score);
          } else {
            int prizeResult = pickupPrize();
            score += prizeResult;
            if (prizeResult > 0) {
              JOptionPane.showMessageDialog(frame, "Score: " + score);
            } else {
              JOptionPane.showMessageDialog(frame, "No coin here! Score: " + score);
            }
          }
        }
        else if (key == KeyEvent.VK_Q) {
          if (isOnFakeCoin()) {
            fakeCoinActive = false;
            repaint();
            JOptionPane.showMessageDialog(frame, "Trap disarmed and fake coin removed!");
          }
        }
        else if (key == KeyEvent.VK_F) {
          if (scanCount > 0) {
            scanCount--;
            if (isFakeCoinAhead()) {
              JOptionPane.showMessageDialog(frame, "Scan: FAKE COIN detected ahead!");
            } else {
              JOptionPane.showMessageDialog(frame, "Scan: No fake coin ahead.");
            }
          } else {
            JOptionPane.showMessageDialog(frame, "No scans left!");
          }
        }

        if (moved) {
          movesLeft--;
          score += moveResult;
          if (moveResult < 0) {
            JOptionPane.showMessageDialog(frame, "Invalid move! Score: " + score);
          }
        }

        scoreLabel.setText("Score: " + score);
        movesLabel.setText("Moves left: " + movesLeft);
        timerLabel.setText("Time left: " + timeLeft + "s");
        scansLabel.setText("Scans left: " + scanCount);

        // Check for lose conditions
        if (score <= 0) {
          JOptionPane.showMessageDialog(frame, "Game Over! You ran out of points.");
          endGame();
        } else if (movesLeft <= 0) {
          JOptionPane.showMessageDialog(frame, "Game Over! You ran out of moves.");
          endGame();
        } else if (timerExpired) {
          JOptionPane.showMessageDialog(frame, "Game Over! Time's up.");
          endGame();
        } else if (allPrizesCollected()) {
          JOptionPane.showMessageDialog(frame, "Congratulations! You collected all the coins and finished the game.");
          endGame();
        }
      }
    });
  }

 /**
  * After a GameGUI object is created, this method adds the walls, prizes, and traps to the gameboard.
  * Note that traps and prizes may occupy the same location.
  */
  public void createBoard()
  {
    traps = new Rectangle[totalTraps];
    createTraps();
    prizes = new Rectangle[totalPrizes];
    createPrizes();
    walls = new Rectangle[totalWalls];
    createWalls();
    fakeCoin = new Rectangle(START_LOC_X + SPACE_SIZE * 3, START_LOC_Y + SPACE_SIZE * 2, 15, 15); // Example location
    fakeCoinActive = true;
  }

  /**
   * Increment/decrement the player location by the amount designated.
   * This method checks for bumping into walls and going off the grid,
   * both of which result in a penalty.
   * <P>
   * precondition: amount to move is not larger than the board, otherwise player may appear to disappear
   * postcondition: increases number of steps even if the player did not actually move (e.g. bumping into a wall)
   * <P>
   * @param incrx amount to move player in x direction
   * @param incry amount to move player in y direction
   * @return penalty score for hitting a wall or potentially going off the grid, 0 otherwise
   */
  public int movePlayer(int incrx, int incry)
  {
      int newX = x + incrx;
      int newY = y + incry;
      
      playerSteps++;

      // check if off grid horizontally and vertically
      if ( (newX < 0 || newX > WIDTH-SPACE_SIZE) || (newY < 0 || newY > HEIGHT-SPACE_SIZE) )
      {
        System.out.println ("OFF THE GRID!");
        score -= 10;
        return -offGridVal;
      }

      // determine if a wall is in the way
      for (Rectangle r: walls)
      {
        int startX =  (int)r.getX();
        int endX  =  (int)r.getX() + (int)r.getWidth();
        int startY =  (int)r.getY();
        int endY = (int) r.getY() + (int)r.getHeight();

        if ((incrx > 0) && (x <= startX) && (startX <= newX) && (y >= startY) && (y <= endY))
        {
          System.out.println("A WALL IS IN THE WAY");
          return -hitWallVal;
        }
        else if ((incrx < 0) && (x >= startX) && (startX >= newX) && (y >= startY) && (y <= endY))
        {
          System.out.println("A WALL IS IN THE WAY");
          return -hitWallVal;
        }
        else if ((incry > 0) && (y <= startY && startY <= newY && x >= startX && x <= endX))
        {
          System.out.println("A WALL IS IN THE WAY");
          return -hitWallVal;
        }
        else if ((incry < 0) && (y >= startY) && (startY >= newY) && (x >= startX) && (x <= endX))
        {
          System.out.println("A WALL IS IN THE WAY");
          return -hitWallVal;
        }     
      }
      x += incrx;
      y += incry;
      repaint();   
      return 0;   
  }

  /**
   * Check the space adjacent to the player for a trap. The adjacent location is one space away from the player, 
   * designated by newx, newy.
   * <P>
   * precondition: newx and newy must be the amount a player regularly moves, otherwise an existing trap may go undetected
   * <P>
   * @param newx a location indicating the space to the right or left of the player
   * @param newy a location indicating the space above or below the player
   * @return true if the new location has a trap that has not been sprung, false otherwise
   */
  public boolean isTrap(int newx, int newy)
  {
    double px = playerLoc.getX() + newx;
    double py = playerLoc.getY() + newy;


    for (Rectangle r: traps)
    {
      if (r.getWidth() > 0)
      {
        // if new location of player has a trap, return true
        if (r.contains(px, py))
        {
          System.out.println("A TRAP IS AHEAD");
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Spring the trap. Traps can only be sprung once and attempts to spring
   * a sprung task results in a penalty.
   * <P>
   * precondition: newx and newy must be the amount a player regularly moves, otherwise an existing trap may go unsprung
   * <P>
   * @param newx a location indicating the space to the right or left of the player
   * @param newy a location indicating the space above or below the player
   * @return a positive score if a trap is sprung, otherwise a negative penalty for trying to spring a non-existent trap
   */
  public int springTrap(int newx, int newy)
  {
    double px = playerLoc.getX() + newx;
    double py = playerLoc.getY() + newy;

    for (Rectangle r: traps)
    {
      if (r.contains(px, py))
      {
        if (r.getWidth() > 0)
        {
          r.setSize(0,0);
          System.out.println("TRAP IS SPRUNG!");
          return trapVal;
        }
      }
    }
    System.out.println("THERE IS NO TRAP HERE TO SPRING");
    return -trapVal;
  }

  /**
   * Pickup a prize and score points. If no prize is in that location, this results in a penalty.
   * <P>
   * @return positive score if a location had a prize to be picked up, otherwise a negative penalty
   */
  public int pickupPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle p: prizes)
    {
      // if location has a coin, pick it up
      if (p.getWidth() > 0 && p.contains(px, py))
      {
        System.out.println("YOU PICKED UP A COIN!");
        p.setSize(0,0);
        repaint();
        return prizeVal;
      }
    }
    System.out.println("OOPS, NO COIN HERE");
    return -prizeVal;  
    
  }

  /**
   * Return the numbers of steps the player has taken.
   * <P>
   * @return the number of steps
   */
  public int getSteps()
  {
    return playerSteps;
  }
  
  /**
   * Set the designated number of prizes in the game.  This can be used to customize the gameboard configuration.
   * <P>
   * precondition p must be a positive, non-zero integer
   * <P>
   * @param p number of prizes to create
   */
  public void setPrizes(int p) 
  {
    totalPrizes = p;
  }
  
  /**
   * Set the designated number of traps in the game. This can be used to customize the gameboard configuration.
   * <P>
   * precondition t must be a positive, non-zero integer
   * <P>
   * @param t number of traps to create
   */
  public void setTraps(int t) 
  {
    totalTraps = t;
  }
  
  /**
   * Set the designated number of walls in the game. This can be used to customize the gameboard configuration.
   * <P>
   * precondition t must be a positive, non-zero integer
   * <P>
   * @param w number of walls to create
   */
  public void setWalls(int w) 
  {
    totalWalls = w;
  }

  /**
   * Reset the board to replay existing game. The method can be called at any time but results in a penalty if called
   * before the player reaches the far right wall.
   * <P>
   * @return positive score for reaching the far right wall, penalty otherwise
   */
  public int replay()
  {

    int win = playerAtEnd();
  
    // resize prizes and traps to "reactivate" them
    for (Rectangle p: prizes)
      p.setSize(SPACE_SIZE/3, SPACE_SIZE/3);
    for (Rectangle t: traps)
      t.setSize(SPACE_SIZE/3, SPACE_SIZE/3);

    // move player to start of board
    x = START_LOC_X;
    y = START_LOC_Y;
    playerSteps = 0;
    repaint();
    return win;
  }

 /**
  * End the game, checking if the player made it to the far right wall.
  * <P>
  * @return positive score for reaching the far right wall, penalty otherwise
  */
  public int endGame() 
  {
    int win = playerAtEnd();
  
    setVisible(false);
    frame.dispose();
    return win;
  }

  /*------------------- public methods not to be called as part of API -------------------*/

  /** 
   * For internal use and should not be called directly: Users graphics buffer to paint board elements.
   */
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g;

    // draw grid
    g.drawImage(bgImage, 0, 0, null);

    // add (invisible) traps
    for (Rectangle t : traps)
    {
      g2.setPaint(Color.WHITE); 
      g2.fill(t);
    }

    // add prizes
    for (Rectangle p : prizes)
    {
      if (p.getWidth() > 0) 
      {
        int px = (int)p.getX();
        int py = (int)p.getY();
        g.drawImage(prizeImage, px, py, null);
      }
    }
    // draw fake coin trap if active
    if (fakeCoinActive) {
      int fx = (int)fakeCoin.getX();
      int fy = (int)fakeCoin.getY();
      g.drawImage(prizeImage, fx, fy, null); // Use prize image for fake coin
    }
    // add walls
    for (Rectangle r : walls) 
    {
      g2.setPaint(Color.BLACK);
      g2.fill(r);
    }
   
    // draw player, saving its location
    g.drawImage(player, x, y, 40,40, null);
    playerLoc.setLocation(x,y);

    // (do not draw stats here, as they are now in the right panel)
  }

  /*------------------- private methods -------------------*/

  /*
   * Add randomly placed prizes to be picked up.
   * Note:  prizes and traps may occupy the same location, with traps hiding prizes
   */
  private void createPrizes()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
     for (int numPrizes = 0; numPrizes < totalPrizes; numPrizes++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
      prizes[numPrizes] = r;
     }
  }

  /*
   * Add randomly placed traps to the board. They will be painted white and appear invisible.
   * Note:  prizes and traps may occupy the same location, with traps hiding prizes
   */
  private void createTraps()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
     for (int numTraps = 0; numTraps < totalTraps; numTraps++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
      traps[numTraps] = r;
     }
  }

  /*
   * Add walls to the board in random locations 
   */
  private void createWalls()
  {
     int s = SPACE_SIZE; 

     Random rand = new Random();
     for (int numWalls = 0; numWalls < totalWalls; numWalls++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
       if (rand.nextInt(2) == 0) 
       {
         // vertical wall
         r = new Rectangle((w*s + s - 5),h*s, 8,s);
       }
       else
       {
         /// horizontal
         r = new Rectangle(w*s,(h*s + s - 5), s, 8);
       }
       walls[numWalls] = r;
     }
  }

  /**
   * Checks if player as at the far right of the board 
   * @return positive score for reaching the far right wall, penalty otherwise
   */
  private int playerAtEnd() 
  {
    int result;

    double px = playerLoc.getX();
    if (px > (WIDTH - 2*SPACE_SIZE))
    {
      System.out.println("YOU MADE IT!");
      result = endVal;
    }
    else
    {
      System.out.println("OOPS, YOU LOSE!, Final Score: " + score);
      result = -endVal;
    }
    return result;
  
  }

  // Helper to check if all prizes are collected
  private boolean allPrizesCollected() {
    for (Rectangle p : prizes) {
      if (p.getWidth() > 0) return false;
    }
    return true;
  }

  //helper to check if player is on the fake coin
  private boolean isOnFakeCoin() {
    return fakeCoinActive && fakeCoin.contains(x, y);
  }



  //scan to check if fake coin is ahead of player
  private boolean isFakeCoinAhead() {
    //check in one square ahead in each direction
    int[][] dirs = { {SPACE_SIZE,0}, {-SPACE_SIZE,0}, {0,SPACE_SIZE}, {0,-SPACE_SIZE} };
    for (int[] d : dirs) {
      int nx = x + d[0];
      int ny = y + d[1];
      if (fakeCoinActive && fakeCoin.contains(nx, ny)) return true;
    }
    return false;
  }
}
