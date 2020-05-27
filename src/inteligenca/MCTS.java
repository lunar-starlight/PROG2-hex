package inteligenca;

import logika.Igra;
import logika.Player;
import splosno.Koordinati;
import inteligenca.Node;
import inteligenca.Naive;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class MCTS {
    final static int INF = Integer.MAX_VALUE;

    private static Player player;
    private static Map<Igra, Node> visited_nodes = new HashMap<Igra, Node>();

    public MCTS(Player player) {
        MCTS.player = player;
    }

    private static Node selectFavouriteChild(Node parent) {
        Set<Node> children = parent.children;
        Node favorite_child = null;
        double max_score = -INF;
        for (Node child : children) {
            if (child.UCB_score(parent.visits, player) > max_score) {
                max_score = child.UCB_score(parent.visits, player);
                favorite_child = child;
            }
        }
        return favorite_child;
    }

    private static Node expand(Node parent) {
        for (Koordinati move : parent.igra.possibleMoves()) {
            Igra igra = new Igra(parent.igra);
            igra.odigraj(move);
            parent.children.add(new Node(igra, parent, parent.prior_probability));
        }
        visited_nodes.put(parent.igra, parent);
        return parent;
    }

    private static double simulate(Node child) {
        Igra igra = new Igra(child.igra);
        while (igra.status == Igra.Status.IN_PROGRESS) {
            Koordinati move = Naive.play(igra);
            igra.odigraj(move);
        }
        if (igra.status.winner == player) {
            return 1;
        }
        return -1;
    }

    private static void backprop(Node selected, Node root, double outcome) {
        Node current = selected.parent;
        while (current != root) {
            current.update_value(outcome);
            current = current.parent;
        }
        root.update_value(outcome);
    }

    private static void search(Node root) {
        double outcome = 0;
        Node selected = root;
        Set<Koordinati> moves = selected.igra.possibleMoves();
        // mogoce rabimo met max depth?
        while (selected.children.size() > 0 && selected.igra.status == Igra.Status.IN_PROGRESS) {
            selected = selectFavouriteChild(selected);
            moves = selected.igra.possibleMoves();
        }
        switch (selected.igra.status) {
            case WIN:
                if (selected.igra.status.winner == player)
                    outcome = 1;
                else
                    outcome = -1;
            default: // case IN_PROGRESS:
                selected = expand(selected);
                int rand_int = new Random().nextInt(moves.size());
                int i = 0;
                for (Node child : selected.children) {
                    if (i == rand_int) {
                        outcome = simulate(child);
                        break;
                    }
                    i++;
                }
        }
        backprop(selected, root, outcome);
    }

    public static Koordinati play(Igra igra) {
        Node origin;
        if (visited_nodes.containsKey(igra)) {
            origin = visited_nodes.get(igra);
        } else {
            origin = new Node(igra, null, 0);
            origin = expand(origin);
            origin.value = simulate(origin);
        }
        search(origin);
        Koordinati move = selectFavouriteChild(origin).igra.getLastMove();
        return move;
    }
}