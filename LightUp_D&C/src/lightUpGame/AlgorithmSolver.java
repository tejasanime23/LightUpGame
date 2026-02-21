package lightUpGame;

import java.util.*;

class AlgorithmSolver {

    public enum AlgoType { ALGO1, ALGO2 }
    private AlgoType algoType = AlgoType.ALGO1;

    public void setAlgorithm(AlgoType t) {
        this.algoType = t;
        System.out.println("Algorithm set to: " + t);
    }
    public AlgoType getAlgorithm() { return algoType; }

    private GameBoard board;
    private int size;

    public AlgorithmSolver(GameBoard board) {
        this.board = board;
        this.size  = board.getSize();
    }


    public boolean isValidBulbPlacement(int row, int col) {
        if (isVisibleToAnotherBulb(row, col)) {
            System.out.println("Invalid: bulb at (" + row + "," + col + ") sees another bulb.");
            return false;
        }
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int r = row + d[0], c = col + d[1];
            if (board.isValidCell(r, c) && board.getCellType(r, c) == CellType.NUMBERED) {
                if (countPlacedBulbsAround(r, c) + 1 > board.getNumberValue(r, c)) {
                    System.out.println("Invalid: violates number at (" + r + "," + c + ").");
                    return false;
                }
            }
        }
        return true;
    }

    public void updateAfterBulbPlacement(int row, int col) {
        updateIllumination();
    }

    public Point findOptimalBulbPlacement() {
        if (algoType == AlgoType.ALGO1) return findWithAlgo1();
        else                             return findWithAlgo2();
    }

    public boolean isGameComplete() {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.EMPTY && !board.isLit(r, c))
                    return false;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.NUMBERED)
                    if (countPlacedBulbsAround(r, c) != board.getNumberValue(r, c))
                        return false;
        return true;
    }


    private Point findWithAlgo1() {
        // Step 1: forced constraint propagation on live board
        Point forced = forcedConstraintPropagation();
        if (forced != null) { System.out.println("[A1] Forced: " + forced); return forced; }

        System.out.println("[A1] No forced moves – starting D&C …");
        GridState init = new GridState(board);
        GridState result = algo1FindSafeBulb(init, 0, 0, size, size);
        if (result != null) {
            for (int r = 0; r < size; r++)
                for (int c = 0; c < size; c++)
                    if (result.hasBulb(r, c) && !board.hasBulb(r, c))
                        return new Point(r, c);
        }
        System.out.println("[A1] No solution found.");
        return null;
    }


    private Point forcedConstraintPropagation() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (board.getCellType(r, c) != CellType.NUMBERED) continue;
                    int k = board.getNumberValue(r, c);
                    int placed = countPlacedBulbsAround(r, c);
                    int free   = countFreeNeighbours(r, c);
                    if (placed > k) return null;                          // Rule B
                    if (placed == k && free > 0)
                        if (forbidFreeNeighbours(r, c)) changed = true;   // Rule C
                    int needed = k - placed;
                    if (needed > 0 && needed == free) {                   // Rule A
                        List<Point> nb = getFreeNeighbours(r, c);
                        if (!nb.isEmpty()) return nb.get(0);
                    }
                }
            }
            for (int r = 0; r < size; r++) {                              // Rule D
                for (int c = 0; c < size; c++) {
                    if (board.getCellType(r, c) == CellType.EMPTY
                            && !board.isLit(r, c) && !board.hasBulb(r, c)) {
                        List<Point> srcs = reachableSources(r, c);
                        if (srcs.isEmpty()) return null;
                        if (srcs.size() == 1) {
                            Point s = srcs.get(0);
                            if (!board.hasBulb(s.x, s.y) && !board.isBlocked(s.x, s.y))
                                return s;
                        }
                    }
                }
            }
        }
        return null;
    }


    private GridState algo1FindSafeBulb(GridState st, int rOff, int cOff, int h, int w) {
        if (!st.deduceLocal(rOff, cOff, h, w)) return null;
        if (h <= 2 || w <= 2) return algo1BaseCase(st, rOff, cOff, h, w);

        boolean horiz = a1ChooseOrientation(st, rOff, cOff, h, w);
        int sep = a1ChooseSeparator(st, rOff, cOff, h, w, horiz);
        if (sep == -1) sep = horiz ? (rOff + h / 2) : (cOff + w / 2);

        List<Point> sepCells = new ArrayList<>();
        if (horiz) {
            for (int j = cOff; j < cOff + w; j++)
                if (st.getCellType(sep, j) == CellType.EMPTY) sepCells.add(new Point(sep, j));
        } else {
            for (int i = rOff; i < rOff + h; i++)
                if (st.getCellType(i, sep) == CellType.EMPTY) sepCells.add(new Point(i, sep));
        }

        int n = sepCells.size();
        if (n > 8) return null; 

        long total = 1;
        for (int i = 0; i < n; i++) total *= 4;

        for (long s = 0; s < total; s++) {
            GridState cand = new GridState(st);
            long tmp = s; boolean ok = true;
            for (Point p : sepCells) {
                if (!a1ApplyIS(cand, p, (int)(tmp % 4), horiz)) { ok = false; break; }
                tmp /= 4;
            }
            if (!ok) continue;
            if (!cand.deduceLocal(rOff, cOff, h, w)) continue;

            GridState r1, r2;
            if (horiz) {
                r1 = algo1FindSafeBulb(cand, rOff, cOff, sep - rOff, w);
                if (r1 == null) continue;
                r2 = algo1FindSafeBulb(r1, sep + 1, cOff, rOff + h - sep - 1, w);
            } else {
                r1 = algo1FindSafeBulb(cand, rOff, cOff, h, sep - cOff);
                if (r1 == null) continue;
                r2 = algo1FindSafeBulb(r1, rOff, sep + 1, h, cOff + w - sep - 1);
            }
            if (r2 != null && r2.isGloballyConsistent()) return r2;
        }
        System.out.println("[A1] All IS exhausted for sep=" + sep);
        return null;
    }

    private boolean a1ApplyIS(GridState st, Point p, int is, boolean horiz) {
        int r = p.x, c = p.y;
        switch (is) {
            case 0:
                if (st.hasBulb(r, c) || st.isLit(r, c)) return false;
                st.setBlocked(r, c, true); return true;
            case 1:
                if (!st.canPlaceBulb(r, c)) return false;
                st.placeBulb(r, c); return true;
            case 2:
                if (st.hasBulb(r, c)) return false;
                st.setLit(r, c, true); st.setBlocked(r, c, true);
                return horiz ? st.propagateRay(r, c, +1, 0) : st.propagateRay(r, c, 0, +1);
            case 3:
                if (st.hasBulb(r, c)) return false;
                st.setLit(r, c, true); st.setBlocked(r, c, true);
                return horiz ? st.propagateRay(r, c, -1, 0) : st.propagateRay(r, c, 0, -1);
            default: return false;
        }
    }

    private GridState algo1BaseCase(GridState st, int rOff, int cOff, int h, int w) {
        GridState cur = new GridState(st);
        for (int r = rOff; r < rOff + h; r++) {
            for (int c = cOff; c < cOff + w; c++) {
                if (cur.getCellType(r, c) != CellType.EMPTY) continue;
                if (cur.hasBulb(r, c) || cur.isLit(r, c))   continue;
                if (cur.canPlaceBulb(r, c) && !cur.localViolation(r, c)) {
                    GridState next = new GridState(cur);
                    next.placeBulb(r, c);
                    if (!next.globalViolation()) cur = next; // greedy accept
                }
            }
        }
        return cur;
    }

    private boolean a1ChooseOrientation(GridState st, int rOff, int cOff, int h, int w) {
        int mr = 0, mc = 0;
        for (int r = rOff + 1; r < rOff + h - 1; r++) {
            int cnt = 0;
            for (int c = cOff; c < cOff + w; c++) if (st.getCellType(r, c) != CellType.EMPTY) cnt++;
            if (cnt > mr) mr = cnt;
        }
        for (int c = cOff + 1; c < cOff + w - 1; c++) {
            int cnt = 0;
            for (int r = rOff; r < rOff + h; r++) if (st.getCellType(r, c) != CellType.EMPTY) cnt++;
            if (cnt > mc) mc = cnt;
        }
        return mr >= mc;
    }

    private int a1ChooseSeparator(GridState st, int rOff, int cOff, int h, int w, boolean horiz) {
        int best = -1, bB = -1, bC = -1;
        if (horiz) {
            for (int r = rOff + 1; r < rOff + h - 1; r++) {
                int blacks = 0, clue = 0;
                for (int c = cOff; c < cOff + w; c++) {
                    CellType ct = st.getCellType(r, c);
                    if (ct != CellType.EMPTY) { blacks++; if (ct == CellType.NUMBERED && st.getNumber(r,c)>clue) clue=st.getNumber(r,c); }
                }
                if (blacks > bB || (blacks == bB && clue > bC)) { bB=blacks; bC=clue; best=r; }
            }
        } else {
            for (int c = cOff + 1; c < cOff + w - 1; c++) {
                int blacks = 0, clue = 0;
                for (int r = rOff; r < rOff + h; r++) {
                    CellType ct = st.getCellType(r, c);
                    if (ct != CellType.EMPTY) { blacks++; if (ct == CellType.NUMBERED && st.getNumber(r,c)>clue) clue=st.getNumber(r,c); }
                }
                if (blacks > bB || (blacks == bB && clue > bC)) { bB=blacks; bC=clue; best=c; }
            }
        }
        return best;
    }


    private static final List<Set<Point>> NO_SOLUTION    = Collections.emptyList();
    private static final List<Set<Point>> EMPTY_CANDIDATE =
            Collections.singletonList(Collections.emptySet());

    private Point findWithAlgo2() {
        System.out.println("[A2] Starting Candidate-Set D&C …");
        GridState init = new GridState(board);
        List<Set<Point>> candidates = algo2Solve(init, 0, 0, size, size);

        if (!candidates.isEmpty()) {
            Set<Point> best = algo2Best(candidates, init);
            for (Point p : best)
                if (!board.hasBulb(p.x, p.y)) { System.out.println("[A2] Move: " + p); return p; }
        }
        System.out.println("[A2] No solution found.");
        return null;
    }

    /** Steps 1-9 of Algorithm 2. Returns a list of candidate placements. */
    private List<Set<Point>> algo2Solve(GridState st, int rOff, int cOff, int h, int w) {
        if (h <= 0 || w <= 0) return EMPTY_CANDIDATE;

        // Step 5: Base case
        if (h <= 2 || w <= 2) return algo2BaseCase(st, rOff, cOff, h, w);

        // Step 1: Choose separator
        boolean horiz = a2ChooseOrientation(st, rOff, cOff, h, w);
        int sep = a2ChooseSeparator(st, rOff, cOff, h, w, horiz);
        if (sep == -1) sep = horiz ? (rOff + h / 2) : (cOff + w / 2);

        // Step 2: Forced fill on separator only
        GridState filled = new GridState(st);
        if (!a2ForcedFill(filled, sep, horiz, rOff, cOff, h, w)) {
            System.out.println("[A2] Contradiction in forced fill at sep=" + sep);
            return NO_SOLUTION;
        }

        // Collect bulbs newly placed by forced fill on the separator line
        Set<Point> sepBulbs = new HashSet<>();
        if (horiz) {
            for (int c = cOff; c < cOff + w; c++)
                if (filled.hasBulb(sep, c) && !st.hasBulb(sep, c)) sepBulbs.add(new Point(sep, c));
        } else {
            for (int r = rOff; r < rOff + h; r++)
                if (filled.hasBulb(r, sep) && !st.hasBulb(r, sep)) sepBulbs.add(new Point(r, sep));
        }

        // Steps 3 & 4: Divide and recurse
        List<Set<Point>> cu, cd;
        if (horiz) {
            int hu = sep - rOff, hd = rOff + h - sep - 1;
            cu = (hu > 0) ? algo2Solve(filled, rOff,    cOff, hu, w) : EMPTY_CANDIDATE;
            cd = (hd > 0) ? algo2Solve(filled, sep + 1, cOff, hd, w) : EMPTY_CANDIDATE;
        } else {
            int wl = sep - cOff, wr = cOff + w - sep - 1;
            cu = (wl > 0) ? algo2Solve(filled, rOff, cOff,    h, wl) : EMPTY_CANDIDATE;
            cd = (wr > 0) ? algo2Solve(filled, rOff, sep + 1, h, wr) : EMPTY_CANDIDATE;
        }

        // Step 6: Compatibility merge
        List<Set<Point>> C = new ArrayList<>();
        for (Set<Point> u : cu) {
            for (Set<Point> d : cd) {
                Set<Point> merged = new HashSet<>(u);
                merged.addAll(d);
                merged.addAll(sepBulbs);
                if (a2Compatible(merged, filled)) C.add(merged);
            }
        }

        // Step 7: Greedy pruning
        if (C.size() > 20) C = algo2Prune(C, filled, 5);

        // Step 8: return
        return C.isEmpty() ? NO_SOLUTION : C;
    }

    private boolean a2ChooseOrientation(GridState st, int rOff, int cOff, int h, int w) {
        int mr = 0, mc = 0;
        for (int r = rOff + 1; r < rOff + h - 1; r++) {
            int s = 0;
            for (int c = cOff; c < cOff + w; c++) {
                CellType ct = st.getCellType(r, c);
                if (ct == CellType.NUMBERED) s += st.getNumber(r,c) + 2;
                else if (ct == CellType.BLACK) s++;
            }
            if (s > mr) mr = s;
        }
        for (int c = cOff + 1; c < cOff + w - 1; c++) {
            int s = 0;
            for (int r = rOff; r < rOff + h; r++) {
                CellType ct = st.getCellType(r, c);
                if (ct == CellType.NUMBERED) s += st.getNumber(r,c) + 2;
                else if (ct == CellType.BLACK) s++;
            }
            if (s > mc) mc = s;
        }
        return mr >= mc;
    }

    private int a2ChooseSeparator(GridState st, int rOff, int cOff, int h, int w, boolean horiz) {
        int best = -1, bestScore = -1;
        if (horiz) {
            for (int r = rOff + 1; r < rOff + h - 1; r++) {
                int score = 0;
                for (int c = cOff; c < cOff + w; c++) {
                    CellType ct = st.getCellType(r, c);
                    if (ct == CellType.NUMBERED) score += st.getNumber(r,c) + 2;
                    else if (ct == CellType.BLACK) score++;
                    // Bonus: adjacent numbered rows
                    if (st.isInBounds(r-1,c) && st.getCellType(r-1,c)==CellType.NUMBERED) score += st.getNumber(r-1,c);
                    if (st.isInBounds(r+1,c) && st.getCellType(r+1,c)==CellType.NUMBERED) score += st.getNumber(r+1,c);
                }
                if (score > bestScore) { bestScore = score; best = r; }
            }
        } else {
            for (int c = cOff + 1; c < cOff + w - 1; c++) {
                int score = 0;
                for (int r = rOff; r < rOff + h; r++) {
                    CellType ct = st.getCellType(r, c);
                    if (ct == CellType.NUMBERED) score += st.getNumber(r,c) + 2;
                    else if (ct == CellType.BLACK) score++;
                    if (st.isInBounds(r,c-1) && st.getCellType(r,c-1)==CellType.NUMBERED) score += st.getNumber(r,c-1);
                    if (st.isInBounds(r,c+1) && st.getCellType(r,c+1)==CellType.NUMBERED) score += st.getNumber(r,c+1);
                }
                if (score > bestScore) { bestScore = score; best = c; }
            }
        }
        return best;
    }


    private boolean a2ForcedFill(GridState st, int sep, boolean horiz, int rOff, int cOff, int h, int w) {
        boolean changed = true;
        while (changed) {
            changed = false;
            if (horiz) {
                for (int c = cOff; c < cOff + w; c++) {
                    // numbered cell above separator
                    if (st.isInBounds(sep-1,c) && st.getCellType(sep-1,c)==CellType.NUMBERED)
                        { Boolean r = a2ApplyFill(st, sep-1, c, sep, true); if (r==null) return false; if (r) changed=true; }
                    // numbered cell below separator
                    if (st.isInBounds(sep+1,c) && st.getCellType(sep+1,c)==CellType.NUMBERED)
                        { Boolean r = a2ApplyFill(st, sep+1, c, sep, true); if (r==null) return false; if (r) changed=true; }
                    // numbered cell ON separator
                    if (st.getCellType(sep,c)==CellType.NUMBERED)
                        { Boolean r = a2ApplyAllNeighbours(st, sep, c); if (r==null) return false; if (r) changed=true; }
                }
            } else {
                for (int r = rOff; r < rOff + h; r++) {
                    if (st.isInBounds(r,sep-1) && st.getCellType(r,sep-1)==CellType.NUMBERED)
                        { Boolean res = a2ApplyFill(st, r, sep-1, sep, false); if (res==null) return false; if (res) changed=true; }
                    if (st.isInBounds(r,sep+1) && st.getCellType(r,sep+1)==CellType.NUMBERED)
                        { Boolean res = a2ApplyFill(st, r, sep+1, sep, false); if (res==null) return false; if (res) changed=true; }
                    if (st.getCellType(r,sep)==CellType.NUMBERED)
                        { Boolean res = a2ApplyAllNeighbours(st, r, sep); if (res==null) return false; if (res) changed=true; }
                }
            }
        }
        return true;
    }

    /** Returns null=contradiction, true=change made, false=no change. */
    private Boolean a2ApplyFill(GridState st, int numR, int numC, int sepLine, boolean horizSep) {
        int k = st.getNumber(numR, numC);
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        int b = 0;
        List<Point> sepFree = new ArrayList<>();
        for (int[] d : dirs) {
            int nr = numR+d[0], nc = numC+d[1];
            if (!st.isInBounds(nr,nc)) continue;
            if (st.getCellType(nr,nc) != CellType.EMPTY) continue;
            if (st.hasBulb(nr,nc)) { b++; continue; }
            if (st.isBlocked(nr,nc)) continue;
            boolean onSep = horizSep ? (nr == sepLine) : (nc == sepLine);
            if (onSep) sepFree.add(new Point(nr,nc));
        }
        if (b > k) return null; // contradiction
        boolean changed = false;
        // Rule A on separator cells
        if (!sepFree.isEmpty() && b + sepFree.size() == k) {
            for (Point p : sepFree) {
                if (!st.hasBulb(p.x,p.y) && st.canPlaceBulb(p.x,p.y)) {
                    st.placeBulb(p.x,p.y); changed = true;
                }
            }
        }
        // Rule C on separator cells
        if (b == k) {
            for (Point p : sepFree) {
                if (!st.isBlocked(p.x,p.y)) { st.setBlocked(p.x,p.y,true); changed = true; }
            }
        }
        return changed;
    }

    /** Apply rule A and C considering ALL neighbours (for numbered cells on the separator itself). */
    private Boolean a2ApplyAllNeighbours(GridState st, int r, int c) {
        int k = st.getNumber(r,c);
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        int b = 0; List<Point> free = new ArrayList<>();
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            if (!st.isInBounds(nr,nc)) continue;
            if (st.getCellType(nr,nc) != CellType.EMPTY) continue;
            if (st.hasBulb(nr,nc)) { b++; continue; }
            if (!st.isBlocked(nr,nc)) free.add(new Point(nr,nc));
        }
        if (b > k) return null;
        boolean changed = false;
        if (b + free.size() == k) {
            for (Point p : free) if (st.canPlaceBulb(p.x,p.y)) { st.placeBulb(p.x,p.y); changed=true; }
        }
        if (b == k) {
            for (Point p : free) if (!st.isBlocked(p.x,p.y)) { st.setBlocked(p.x,p.y,true); changed=true; }
        }
        return changed;
    }

    // -- A2: Step 5 – Base case (enumerate all valid placements) --------------

    private List<Set<Point>> algo2BaseCase(GridState st, int rOff, int cOff, int h, int w) {
        Set<Point> fixed = new HashSet<>();
        List<Point> candidates = new ArrayList<>();
        for (int r = rOff; r < rOff + h; r++) {
            for (int c = cOff; c < cOff + w; c++) {
                if (st.getCellType(r,c) != CellType.EMPTY) continue;
                if (st.hasBulb(r,c)) { fixed.add(new Point(r,c)); continue; }
                if (!st.isBlocked(r,c)) candidates.add(new Point(r,c));
            }
        }
        int n = candidates.size();
        if (n > 20) n = 20; // safety cap
        List<Set<Point>> results = new ArrayList<>();
        for (int mask = 0; mask < (1 << n); mask++) {
            Set<Point> placement = new HashSet<>(fixed);
            for (int i = 0; i < n; i++)
                if ((mask & (1 << i)) != 0) placement.add(candidates.get(i));
            if (a2ValidForSubgrid(placement, st, rOff, cOff, h, w)) results.add(placement);
        }
        return results.isEmpty() ? EMPTY_CANDIDATE : results;
    }

    private boolean a2ValidForSubgrid(Set<Point> bulbs, GridState st, int rOff, int cOff, int h, int w) {
        GridState tmp = new GridState(st);
        for (Point p : bulbs) {
            if (tmp.hasBulb(p.x,p.y)) continue;
            if (!tmp.canPlaceBulb(p.x,p.y)) return false;
            tmp.placeBulb(p.x,p.y);
        }
        for (int r = rOff; r < rOff + h; r++)
            for (int c = cOff; c < cOff + w; c++)
                if (tmp.getCellType(r,c)==CellType.NUMBERED && tmp.bulbsAround(r,c) > tmp.getNumber(r,c))
                    return false;
        return true;
    }

    // -- A2: Step 6 – Compatibility check 

    private boolean a2Compatible(Set<Point> merged, GridState base) {
        GridState tmp = new GridState(base);
        for (Point p : merged) {
            if (tmp.hasBulb(p.x,p.y)) continue;
            if (tmp.getCellType(p.x,p.y) != CellType.EMPTY) return false;
            if (!tmp.canPlaceBulb(p.x,p.y)) return false;
            tmp.placeBulb(p.x,p.y);
        }
        return tmp.isGloballyConsistent();
    }

    // -- A2: Step 7 – Greedy pruning & selection 

    private List<Set<Point>> algo2Prune(List<Set<Point>> C, GridState base, int keep) {
        C.sort((a, b) -> algo2Score(b, base) - algo2Score(a, base));
        return new ArrayList<>(C.subList(0, Math.min(keep, C.size())));
    }

    private Set<Point> algo2Best(List<Set<Point>> C, GridState base) {
        return algo2Prune(C, base, 1).get(0);
    }

    private int algo2Score(Set<Point> cand, GridState base) {
        GridState tmp = new GridState(base);
        for (Point p : cand)
            if (!tmp.hasBulb(p.x,p.y) && tmp.getCellType(p.x,p.y)==CellType.EMPTY && tmp.canPlaceBulb(p.x,p.y))
                tmp.placeBulb(p.x,p.y);
        int score = 0;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++) {
                if (tmp.getCellType(r,c)==CellType.NUMBERED) {
                    int nb = tmp.bulbsAround(r,c), req = tmp.getNumber(r,c);
                    score += (nb == req) ? 10 : nb; // Priority 1
                }
                if (tmp.getCellType(r,c)==CellType.EMPTY && tmp.isLit(r,c)) score++; // Priority 2
            }
        return score;
    }


    private void updateIllumination() {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.EMPTY) {
                    board.setLit(r, c, board.hasBulb(r, c));
                    board.setBlocked(r, c, false);
                }
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.hasBulb(r, c)) propagateLightOnBoard(r, c);
    }

    private void propagateLightOnBoard(int r, int c) {
        board.setLit(r, c, true);
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            while (board.isValidCell(nr, nc)) {
                if (board.getCellType(nr,nc)==CellType.BLACK||board.getCellType(nr,nc)==CellType.NUMBERED) break;
                board.setLit(nr,nc,true); board.setBlocked(nr,nc,true);
                nr+=d[0]; nc+=d[1];
            }
        }
    }

    private boolean isVisibleToAnotherBulb(int r, int c) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr=r+d[0], nc=c+d[1];
            while (board.isValidCell(nr,nc)) {
                CellType t = board.getCellType(nr,nc);
                if (t==CellType.BLACK||t==CellType.NUMBERED) break;
                if (board.hasBulb(nr,nc)) return true;
                nr+=d[0]; nc+=d[1];
            }
        }
        return false;
    }

    private int countPlacedBulbsAround(int r, int c) {
        int cnt=0;
        int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
        for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(board.isValidCell(nr,nc)&&board.hasBulb(nr,nc))cnt++;}
        return cnt;
    }
    private int countFreeNeighbours(int r, int c) {
        int cnt=0; int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
        for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(board.isValidCell(nr,nc)&&board.getCellType(nr,nc)==CellType.EMPTY&&!board.hasBulb(nr,nc)&&!board.isBlocked(nr,nc))cnt++;}
        return cnt;
    }
    private List<Point> getFreeNeighbours(int r, int c) {
        List<Point> res=new ArrayList<>(); int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
        for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(board.isValidCell(nr,nc)&&board.getCellType(nr,nc)==CellType.EMPTY&&!board.hasBulb(nr,nc)&&!board.isBlocked(nr,nc))res.add(new Point(nr,nc));}
        return res;
    }
    private boolean forbidFreeNeighbours(int r, int c) {
        boolean ch=false; int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
        for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(board.isValidCell(nr,nc)&&board.getCellType(nr,nc)==CellType.EMPTY&&!board.hasBulb(nr,nc)&&!board.isBlocked(nr,nc)){board.setBlocked(nr,nc,true);ch=true;}}
        return ch;
    }
    private List<Point> reachableSources(int r, int c) {
        List<Point> srcs=new ArrayList<>();
        if(board.getCellType(r,c)==CellType.EMPTY&&!board.hasBulb(r,c)&&!board.isBlocked(r,c)&&isValidBulbPlacement(r,c))srcs.add(new Point(r,c));
        int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
        for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];while(board.isValidCell(nr,nc)){CellType t=board.getCellType(nr,nc);if(t==CellType.BLACK||t==CellType.NUMBERED)break;if(!board.hasBulb(nr,nc)&&!board.isBlocked(nr,nc)&&isValidBulbPlacement(nr,nc))srcs.add(new Point(nr,nc));nr+=d[0];nc+=d[1];}}
        return srcs;
    }


    private class GridState {
        final int[][] grid;      // -1=black, 0-4=numbered, 5=empty
        boolean[][] bulbs;
        boolean[][] lit;
        boolean[][] blocked;
        final int sz;

        GridState(GameBoard b) {
            sz = b.getSize();
            grid = new int[sz][sz]; bulbs = new boolean[sz][sz];
            lit  = new boolean[sz][sz]; blocked = new boolean[sz][sz];
            for (int i = 0; i < sz; i++)
                for (int j = 0; j < sz; j++) {
                    grid[i][j] = b.getNumberValue(i,j);
                    bulbs[i][j] = b.hasBulb(i,j);
                    lit[i][j]   = b.isLit(i,j);
                    blocked[i][j] = b.isBlocked(i,j);
                }
        }
        GridState(GridState o) {
            sz = o.sz;
            grid = new int[sz][sz]; bulbs = new boolean[sz][sz];
            lit  = new boolean[sz][sz]; blocked = new boolean[sz][sz];
            for (int i = 0; i < sz; i++) {
                System.arraycopy(o.grid[i],    0, grid[i],    0, sz);
                System.arraycopy(o.bulbs[i],   0, bulbs[i],   0, sz);
                System.arraycopy(o.lit[i],     0, lit[i],     0, sz);
                System.arraycopy(o.blocked[i], 0, blocked[i], 0, sz);
            }
        }

        CellType getCellType(int r, int c) {
            if (grid[r][c]==-1) return CellType.BLACK;
            if (grid[r][c]>=0&&grid[r][c]<=4) return CellType.NUMBERED;
            return CellType.EMPTY;
        }
        int getNumber(int r, int c)       { return grid[r][c]; }
        boolean isInBounds(int r, int c)  { return r>=0&&r<sz&&c>=0&&c<sz; }
        boolean hasBulb(int r, int c)     { return bulbs[r][c]; }
        boolean isLit(int r, int c)       { return lit[r][c]; }
        boolean isBlocked(int r, int c)   { return blocked[r][c]; }
        void setLit(int r, int c, boolean v)     { lit[r][c]=v; }
        void setBlocked(int r, int c, boolean v) { blocked[r][c]=v; }

        boolean canPlaceBulb(int r, int c) {
            if (getCellType(r,c)!=CellType.EMPTY) return false;
            if (bulbs[r][c]||blocked[r][c]) return false;
            if (visibleToBulb(r,c)) return false;
            if (violatesNumber(r,c)) return false;
            return true;
        }
        void placeBulb(int r, int c) {
            bulbs[r][c]=true; lit[r][c]=true; propagateLight(r,c);
        }
        private void propagateLight(int r, int c) {
            int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];while(isInBounds(nr,nc)){if(getCellType(nr,nc)!=CellType.EMPTY)break;lit[nr][nc]=true;blocked[nr][nc]=true;nr+=d[0];nc+=d[1];}}
        }
        boolean propagateRay(int r, int c, int dr, int dc) {
            int nr=r+dr,nc=c+dc;
            while(isInBounds(nr,nc)){if(getCellType(nr,nc)!=CellType.EMPTY)break;if(bulbs[nr][nc])return false;lit[nr][nc]=true;blocked[nr][nc]=true;nr+=dr;nc+=dc;}
            return true;
        }
        private boolean visibleToBulb(int r, int c) {
            int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];while(isInBounds(nr,nc)){if(getCellType(nr,nc)!=CellType.EMPTY)break;if(bulbs[nr][nc])return true;nr+=d[0];nc+=d[1];}}
            return false;
        }
        private boolean violatesNumber(int r, int c) {
            int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(isInBounds(nr,nc)&&getCellType(nr,nc)==CellType.NUMBERED){int req=grid[nr][nc];int cnt=0;for(int[] d2:dirs){int er=nr+d2[0],ec=nc+d2[1];if(isInBounds(er,ec)&&bulbs[er][ec])cnt++;}if(cnt+1>req)return true;}}
            return false;
        }
        boolean localViolation(int r, int c)  { return visibleToBulb(r,c)||violatesNumber(r,c); }
        boolean globalViolation() {
            for(int r=0;r<sz;r++)for(int c=0;c<sz;c++){if(getCellType(r,c)==CellType.NUMBERED){int req=grid[r][c];int cnt=bulbsAround(r,c);if(cnt>req)return true;}if(getCellType(r,c)==CellType.EMPTY&&bulbs[r][c]&&visibleToBulb(r,c))return true;}
            return false;
        }
        boolean isGloballyConsistent() { return !globalViolation(); }
        int bulbsAround(int r, int c) {
            int cnt=0; int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(isInBounds(nr,nc)&&bulbs[nr][nc])cnt++;}
            return cnt;
        }

        // Deduction engine (used by Algorithm 1 inside the D&C)
        boolean deduceLocal(int rOff, int cOff, int h, int w) {
            boolean changed=true;
            while(changed){changed=false;
                for(int r=rOff;r<rOff+h;r++)for(int c=cOff;c<cOff+w;c++){
                    if(getCellType(r,c)!=CellType.NUMBERED)continue;
                    int k=grid[r][c],placed=bulbsAround(r,c),free=freeNeighbours(r,c);
                    if(placed>k)return false;
                    if(placed==k&&free>0){forbidNeighbours(r,c);changed=true;}
                    int needed=k-placed;
                    if(needed>0&&needed==free){for(Point p:listFreeNeighbours(r,c)){if(!canPlaceBulb(p.x,p.y))return false;placeBulb(p.x,p.y);changed=true;}}
                }
                for(int r=rOff;r<rOff+h;r++)for(int c=cOff;c<cOff+w;c++){
                    if(getCellType(r,c)!=CellType.EMPTY||lit[r][c]||bulbs[r][c])continue;
                    List<Point> srcs=potentialSources(r,c,rOff,cOff,h,w);
                    if(srcs.isEmpty())return false;
                    if(srcs.size()==1){Point s=srcs.get(0);if(!bulbs[s.x][s.y]){if(!canPlaceBulb(s.x,s.y))return false;placeBulb(s.x,s.y);changed=true;}}
                }
            }
            return true;
        }
        private int freeNeighbours(int r, int c) {
            int cnt=0; int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(isInBounds(nr,nc)&&getCellType(nr,nc)==CellType.EMPTY&&!bulbs[nr][nc]&&!blocked[nr][nc])cnt++;}
            return cnt;
        }
        private List<Point> listFreeNeighbours(int r, int c) {
            List<Point> res=new ArrayList<>(); int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(isInBounds(nr,nc)&&getCellType(nr,nc)==CellType.EMPTY&&!bulbs[nr][nc]&&!blocked[nr][nc])res.add(new Point(nr,nc));}
            return res;
        }
        private void forbidNeighbours(int r, int c) {
            int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];if(isInBounds(nr,nc)&&getCellType(nr,nc)==CellType.EMPTY&&!bulbs[nr][nc]&&!blocked[nr][nc])blocked[nr][nc]=true;}
        }
        private List<Point> potentialSources(int r, int c, int rOff, int cOff, int h, int w) {
            List<Point> srcs=new ArrayList<>();
            if(canPlaceBulb(r,c))srcs.add(new Point(r,c));
            int[][] dirs={{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] d:dirs){int nr=r+d[0],nc=c+d[1];while(isInBounds(nr,nc)&&nr>=rOff&&nr<rOff+h&&nc>=cOff&&nc<cOff+w){if(getCellType(nr,nc)!=CellType.EMPTY)break;if(canPlaceBulb(nr,nc))srcs.add(new Point(nr,nc));nr+=d[0];nc+=d[1];}}
            return srcs;
        }
    }
}
