package evalapp.graphgenerator;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

public class GraphVisualizer {

	private DependencyGraph depGraph;

	public GraphVisualizer(DependencyGraph depGraph) {
		this.depGraph = depGraph;
	}

	public void visualize() {
		System.out.println("visualizing");
		Graph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
		for (String node : this.depGraph.getNodes()) {
			graph.addVertex(node);
		}
		for (String sig : this.depGraph.getSignals().keySet()) {
			for (String dep : this.depGraph.getSignalDeps(sig)) {
				graph.addEdge(dep, sig);
			}
		}
		System.out.println(graph.toString());
		for (String n : depGraph.getNodes()) {
			System.out.println(n + " hosted at: " + depGraph.getHost(n));
		}

		/*
		 * DOTExporter<String, DefaultEdge> exporter = new DOTExporter<String,
		 * DefaultEdge>(); try { exporter.exportGraph(graph, new
		 * FileWriter("graph.dot")); } catch (IOException e) {
		 * e.printStackTrace(); }
		 */

	}

}
