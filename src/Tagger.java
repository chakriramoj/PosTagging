import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PosMapping {
	String word;
	String pos;

	PosMapping(String word, String pos) {
		this.pos = pos;
		this.word = word;
	}

	@Override
	public String toString() {
		return "{" + word + "," + pos + "}";
	}

}

public class Tagger {
	static List<String> pOfSpeech = new ArrayList<String>();
	static HashMap<String, Integer> observedVocabulary = new HashMap<String, Integer>();
	static HashMap<String, Integer> noOfTags = new HashMap<String, Integer>();
	static HashMap<String, Integer> trans_tag_counts = new HashMap<String, Integer>();
	static HashMap<String, Integer> emission_counts = new HashMap<String, Integer>();
	static HashMap<String, Double> initial_tag_probabilities = new HashMap<String, Double>();
	static HashMap<String, Double> transition_probabilities = new HashMap<String, Double>();
	static HashMap<String, Double> emission_probabilities = new HashMap<String, Double>();
	static HashMap<String, Integer> count = new HashMap<String, Integer>();

	public static List<List<PosMapping>> load_corpus(String path) throws IOException {
		List<List<PosMapping>> list = new ArrayList<List<PosMapping>>();
		List<PosMapping> lineMapping = new ArrayList<PosMapping>();
		List<String> lines = new ArrayList<String>();
		BufferedReader br=null;
		try {
			br = new BufferedReader(new FileReader(path));
			String line = br.readLine();
			while (line.equals("")) {
				line = br.readLine();
			}
			while (line != null) {
				lines.add(line);
				line = br.readLine();
				while (line != null && line.equals("")) {
					line = br.readLine();
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			br.close();
		}
		for (String a : lines) {
			String[] pairs = a.split("\\s+");
			for (String b : pairs) {
				String pair[] = b.split("/");
				PosMapping mapping = new PosMapping(pair[0], pair[1]);
				lineMapping.add(mapping);
			}
			list.add(new ArrayList<PosMapping>(lineMapping));
			lineMapping.clear();
		}

		return list;
	}

	public static void main(String[] args) throws IOException {
		List<List<PosMapping>> list = load_corpus("ca01");
		System.out.println(list);
		String sentence = "People continue to enquire the";
		initialize_probabilities(list, sentence);
		System.out.println(noOfTags);
		System.out.println(initial_tag_probabilities);
		System.out.println(trans_tag_counts);
		System.out.println(transition_probabilities);
		System.out.println(viterbi_decode(sentence));
	}

	public static void initialize_probabilities(List<List<PosMapping>> list, String sentence) {
		for (List<PosMapping> init : list) {
			for (PosMapping a : init) {
				noOfTags.put(a.pos, noOfTags.getOrDefault(a.pos, 0) + 1);
			}
		}
		for (Map.Entry entry : noOfTags.entrySet()) {
			count.put((String) entry.getKey(), 0);
		}
		for (List<PosMapping> init : list) {
			count.put(init.get(0).pos, count.get(init.get(0).pos) + 1);
		}
		for (Map.Entry entry : count.entrySet()) {
			initial_tag_probabilities.put((String) entry.getKey(),
					((Integer) entry.getValue() + 1) / ((double) list.size() + noOfTags.size()));
		}
		for (List<PosMapping> a : list) {
			for (int i = 0; i < a.size() - 1; i++) {
				trans_tag_counts.put(a.get(i).pos + " " + a.get(i + 1).pos,
						trans_tag_counts.getOrDefault(a.get(i).pos + " " + a.get(i + 1), 0) + 1);
			}
		}
		for (Map.Entry e1 : noOfTags.entrySet()) {
			for (Map.Entry e2 : noOfTags.entrySet()) {
				transition_probabilities.put((String) e1.getKey() + " " + (String) e2.getKey(),
						(trans_tag_counts.getOrDefault((String) e1.getKey() + " " + (String) e2.getKey(), 0) + 1)
								/ ((Integer) e1.getValue() + (double) noOfTags.size()));
			}
		}
		for (List<PosMapping> init : list) {
			for (PosMapping a : init) {
				emission_counts.put(a.pos + " " + a.word, emission_counts.getOrDefault(a.pos + " " + a.word, 0) + 1);
			}
		}
		for (String i : sentence.split("\\s+")) {
			observedVocabulary.put(i, observedVocabulary.getOrDefault(i, 0) + 1);
		}
		for (String k : sentence.split("\\s+")) {
			for (Map.Entry e1 : noOfTags.entrySet()) {
				emission_probabilities.put((String) e1.getKey() + " " + k,
						(emission_counts.getOrDefault((String) e1.getKey() + " " + k, 0) + 1)
								/ ((Integer) e1.getValue() + (double) observedVocabulary.size()));
			}
		}
		for (Map.Entry<String, Integer> entry : noOfTags.entrySet()) {
			pOfSpeech.add(entry.getKey());
		}

	}

	public static List<String> viterbi_decode(String sentence) {
		String[] words = sentence.split("\\s+");
		String[][] list = new String[words.length][pOfSpeech.size()];
		double[][] probability = new double[words.length][pOfSpeech.size()];
		int i = 0;
		while (i < pOfSpeech.size()) {
			probability[0][i] = (initial_tag_probabilities.get(pOfSpeech.get(i))
					* (emission_probabilities.get(pOfSpeech.get(i) + " " + words[0])));
			list[0][i] = pOfSpeech.get(i);
			i++;
		}
		for (int j = 1; j < words.length; j++) {
			for (int k = 0; k < pOfSpeech.size(); k++) {
				double prob = Double.MIN_VALUE;
				int r = 0;
				for (int l = 0; l < pOfSpeech.size(); l++) {
					double curr = (transition_probabilities.get(pOfSpeech.get(l) + " " + pOfSpeech.get(k))
							* emission_probabilities.get(pOfSpeech.get(k) + " " + words[j]) * probability[j - 1][l]);
					if (curr > prob) {
						r = l;
						prob = curr;
					}
				}
				probability[j][k] = prob;
				list[j][k] = list[j - 1][r] + " " + pOfSpeech.get(k);
			}
		}
		double prob = 0;
		String max = "";
		int n = 0;
		while (n < pOfSpeech.size()) {
			if (probability[words.length - 1][n] > prob) {
				prob = probability[words.length - 1][n];
				max = list[words.length - 1][n];
			}
			n++;
		}
		System.out.println(Arrays.deepToString(probability));
		System.out.println(Arrays.deepToString(list));
		List<String> output = new ArrayList<String>(Arrays.asList(max.split("\\s+")));
		return output;
	}
}
