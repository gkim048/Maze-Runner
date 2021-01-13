package cmsc433.p3;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * This file needs to hold your solver to be tested. 
 * You can alter the class to extend any class that extends MazeSolver.
 * It must have a constructor that takes in a Maze.
 * It must have a solve() method that returns the datatype List<Direction>
 *   which will either be a reference to a list of steps to take or will
 *   be null if the maze cannot be solved.
 */
 
public class StudentMTMazeSolver extends SkippingMazeSolver
{
    public StudentMTMazeSolver(Maze maze)
    {
        super(maze);
    }

    public Choice firstChoice(Position pos) throws SolutionFound
    {
        LinkedList<Direction> moves;

        moves = maze.getMoves(pos);
        if (moves.size() == 1) return follow(pos, moves.getFirst());
        else return new Choice(pos, null, moves);
    }

    public Choice follow(Position at, Direction dir) throws SolutionFound
    {
        LinkedList<Direction> choices;
        Direction go_to = dir, came_from = dir.reverse();

        at = at.move(go_to);
        do
        {
            if (at.equals(maze.getEnd())) throw new SolutionFound(at, go_to.reverse());
            if (at.equals(maze.getStart())) throw new SolutionFound(at, go_to.reverse());
            choices = maze.getMoves(at);
            choices.remove(came_from);

        if (choices.size() == 1)
            {
                go_to = choices.getFirst();
                at = at.move(go_to);
                came_from = go_to.reverse();
            }
        } while (choices.size() == 1);

        // return new Choice(at,choices);
        Choice ret = new Choice(at, came_from, choices);
        return ret;
    }

    public List<Direction> solve()
    {
        List<Direction> path = null;

        int process = Runtime.getRuntime().availableProcessors();
        ForkJoinPool fjp = new ForkJoinPool(process);

        try {
            Choice start = firstChoice(maze.getStart());

            while (!start.isDeadend()) {
                Direction choices = start.choices.peek();
                Choice currentChoice = follow(start.at, start.choices.peek());
                start.choices.pop();

                try {
                    path = fjp.submit(new worker(currentChoice, choices)).get();
                    if (path != null) {
                        break;
                    }
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
				    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
				    continue;
                } catch (ExecutionException e) {
                    e.printStackTrace();
				    continue;
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                    continue;
                }
                return null;
            }
        } catch (SolutionFound e) {
            e.printStackTrace();
        }
        fjp.shutdown();
        return path;
    }

    //similar to dfs 
    public class worker implements Callable<List<Direction>> {
        Choice start;
        Direction d;

        public worker(Choice start, Direction d) {
            this.start = start;
            this.d = d;
        }

        public List<Direction> call() {
            LinkedList<Choice> choiceStack = new LinkedList<Choice>();
            Choice ch;

            try {
                choiceStack.push(firstChoice(maze.getStart()));
                while (!choiceStack.isEmpty()) 
                {
                    ch = choiceStack.peek();
                    if (ch.isDeadend()) 
                    {
                        // backtrack.
                        choiceStack.pop();
                        if (!choiceStack.isEmpty()) choiceStack.peek().choices.pop();
                        continue;
                    }
                    choiceStack.push(follow(ch.at, ch.choices.peek()));
                }
                // No solution found.
                return null;

            } 
            catch (SolutionFound e) 
            {
                Iterator<Choice> iter = choiceStack.iterator();
                LinkedList<Direction> solutionPath = new LinkedList<Direction>();
                while (iter.hasNext()) 
                {
                    ch = iter.next();
                    solutionPath.push(ch.choices.peek());
                }

                if (maze.display != null) maze.display.updateDisplay();
                return pathToFullPath(solutionPath);
            }
        }
    }
}