package at.htlhl.graphdemo;

import com.brunomnsilva.smartgraph.containers.ContentZoomScrollPane;
import com.brunomnsilva.smartgraph.graph.Edge;
import com.brunomnsilva.smartgraph.graph.Vertex;
import com.brunomnsilva.smartgraph.graphview.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.util.*;
import java.util.function.Consumer;

public class GraphView extends BorderPane {

    private SmartGraphPanel<VertexData, EdgeData> smartGraphPanel;
    private ContentZoomScrollPane contentZoomScrollPane;
    private GraphControl graphControl;
    private Vertex<VertexData> startVertex = null;
    private Vertex<VertexData> endVertex = null;

    public GraphView(GraphControl graphControl) {
        this.graphControl = graphControl;

        SmartPlacementStrategy strategy = new SmartCircularSortedPlacementStrategy();

        smartGraphPanel = new SmartGraphPanel<>(graphControl.getGraph(), strategy);
        smartGraphPanel.setAutomaticLayout(true);

        contentZoomScrollPane = new ContentZoomScrollPane(smartGraphPanel);

        setCenter(contentZoomScrollPane);

        // Create "Clear Selection" Button
        Button clearSelection = new Button("Clear Selection");
        clearSelection.setOnAction(e -> {
            startVertex = null;
            endVertex = null;

            for (Vertex<VertexData> vertex : graphControl.getGraph().vertices()) {
                smartGraphPanel.getStylableVertex(vertex).setStyleClass("vertex");
            }

            for (Edge<EdgeData, VertexData> edge : graphControl.getGraph().edges()) {
                smartGraphPanel.getStylableEdge(edge).setStyleClass("edge");
            }

            System.out.println("Selection cleared");
        });

        Button findShortestPathButton = new Button("Find Shortest Path");
        findShortestPathButton.setOnAction(e -> {
            if (startVertex != null && endVertex != null) {
                List<Vertex<VertexData>> shortestPath = findShortestPath(startVertex, endVertex);
                highlightPath(shortestPath);
            } else {
                System.out.println("Please select both a start and an end node.");
            }
        });

        // Create ToolBar
        ToolBar toolBar = new ToolBar(clearSelection);
        toolBar.getItems().add(findShortestPathButton);
        setTop(toolBar);

        // Enable double-click on vertex
        smartGraphPanel.setVertexDoubleClickAction(new Consumer<SmartGraphVertex<VertexData>>() {
            @Override
            public void accept(SmartGraphVertex<VertexData> stringSmartGraphVertex) {
                stringSmartGraphVertex.setStyleClass("myVertex");
            }
        });

        // Enable Context Menu on Vertex
        ContextMenu contextMenu = buildContextMenu();

        smartGraphPanel.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent contextMenuEvent) {
                SmartGraphVertexNode<VertexData> foundVertex = findVertexAt(contextMenuEvent.getX(), contextMenuEvent.getY());
                if (foundVertex != null) {
                    contextMenu.show(foundVertex, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
                }
            }
        });
    }

    /**
     * IMPORTANT! Should be called after scene is displayed, so we can initialize the graph visualization.
     */
    public void initAfterVisible() {
        smartGraphPanel.init();
    }

    public ContextMenu buildContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem setStartNodeItem = new MenuItem("Set as Start Node");
        setStartNodeItem.setOnAction(event -> {
            SmartGraphVertexNode<VertexData> vertexNode = (SmartGraphVertexNode<VertexData>) contextMenu.getOwnerNode();
            startVertex = graphControl.getGraph().vertices().stream()
                    .filter(v -> smartGraphPanel.getStylableVertex(v) == vertexNode)
                    .findFirst().orElse(null);
            System.out.println("Start Node: " + startVertex);
            smartGraphPanel.getStylableVertex(startVertex).setStyleClass("highlightStartVertex");
        });

        MenuItem setEndNodeItem = new MenuItem("Set as End Node");
        setEndNodeItem.setOnAction(event -> {
            SmartGraphVertexNode<VertexData> vertexNode = (SmartGraphVertexNode<VertexData>) contextMenu.getOwnerNode();
            endVertex = graphControl.getGraph().vertices().stream()
                    .filter(v -> smartGraphPanel.getStylableVertex(v) == vertexNode)
                    .findFirst().orElse(null);
            System.out.println("End Node: " + endVertex);
            smartGraphPanel.getStylableVertex(endVertex).setStyleClass("highlightEndVertex");

        });

        contextMenu.getItems().addAll(setStartNodeItem, setEndNodeItem);
        return contextMenu;
    }

    private SmartGraphVertexNode<VertexData> findVertexAt(double x, double y) {
        for (Vertex<VertexData> v : graphControl.getGraph().vertices()) {
            SmartStylableNode smartStylableNode = smartGraphPanel.getStylableVertex(v);
            if (smartStylableNode instanceof SmartGraphVertexNode) {
                SmartGraphVertexNode<VertexData> smartGraphVertexNode = (SmartGraphVertexNode) smartStylableNode;
                if (smartGraphVertexNode.getBoundsInParent().contains(x, y)) {
                    return smartGraphVertexNode;
                }
            }
        }
        return null;
    }

    private List<Vertex<VertexData>> findShortestPath(Vertex<VertexData> start, Vertex<VertexData> end) {

        // Maps for distances and vertices
        Map<Vertex<VertexData>, Double> distances = new HashMap<>();
        Map<Vertex<VertexData>, Vertex<VertexData>> previous = new HashMap<>();
        // processes vertices with minimum dist. first
        PriorityQueue<Vertex<VertexData>> queue = new PriorityQueue<>(Comparator.comparing(distances::get));

        // Initialize distances for each vertex to infinity
        for (Vertex<VertexData> vertex : graphControl.getGraph().vertices()) {
            distances.put(vertex, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Vertex<VertexData> current = queue.poll();

            if (current.equals(end)) break;

            // Check neighbour edge
            for (Edge<EdgeData, VertexData> edge : graphControl.getGraph().incidentEdges(current)) {
                Vertex<VertexData> neighbor = graphControl.getGraph().opposite(current, edge);


                double newDist = distances.get(current) + edge.element().getDistance();

                // If the new distance is shorter, update the distance and add the neighbor to the queue
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        // Reconstruct by following the previous map
        List<Vertex<VertexData>> path = new ArrayList<>();
        for (Vertex<VertexData> at = end; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    private void highlightPath(List<Vertex<VertexData>> path) {
        if (path.isEmpty()) {
            System.out.println("No path found.");
            return;
        }

        double totalDistance = 0.0;
        StringBuilder pathString = new StringBuilder();
        pathString.append(path.get(0).element().getName());

        for (int i = 0; i < path.size() - 1; i++) {
            // Edge between Vertex
            Vertex<VertexData> v1 = path.get(i);
            Vertex<VertexData> v2 = path.get(i + 1);

            Edge<EdgeData, VertexData> edge = null;
            for (Edge<EdgeData, VertexData> e : graphControl.getGraph().incidentEdges(v1)) {
                if (graphControl.getGraph().opposite(v1, e).equals(v2)) { //vertex other end of edge
                    edge = e;
                    break;
                }
            }

            if (edge != null) {

                double distance = edge.element().getDistance();
                totalDistance += distance;

                if (pathString.length() > 0) {
                    pathString.append("-");
                }
                pathString.append(v2.element().getName());
                smartGraphPanel.getStylableEdge(edge).setStyleClass("highlighPathtEdge");
            }
        }

        for (Vertex<VertexData> vertex : path) {
            smartGraphPanel.getStylableVertex(vertex).setStyleClass("highlightPathVertex");
        }

        if (startVertex != null) {
            smartGraphPanel.getStylableVertex(startVertex).setStyleClass("highlightStartVertex");
        }
        if (endVertex != null) {
            smartGraphPanel.getStylableVertex(endVertex).setStyleClass("highlightEndVertex");
        }


        Alert alert = new Alert(AlertType.INFORMATION,
                "Result for " + path.get(0).element().getName() + " to " + path.get(path.size() - 1).element().getName() +
                        "\nTotal Distance: " + totalDistance + " km" +
                        "\nPath: " + pathString,
                ButtonType.OK);
        alert.setTitle("Path Information");
        alert.showAndWait();

        System.out.println("Path: " + path);
    }

    private class TestEventHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            System.out.println("Test: " + actionEvent.getSource());
        }
    }
}
