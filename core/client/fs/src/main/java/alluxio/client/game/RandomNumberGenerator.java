package alluxio.client.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yyuau on 17/12/2018.
 * Generate random integers with an arbitrary self-defined distribution
 *
 */
public class RandomNumberGenerator {

  private Map<Integer, Double> distribution;
  private double distSum;

  public RandomNumberGenerator() {
    distribution = new HashMap<>();
  }

  public void addNumber(int value, double probability) {
    if (this.distribution.get(value) != null) {
      distSum -= this.distribution.get(value);
    }
    this.distribution.put(value, probability);
    distSum += probability;
  }

  public int getNext() {
    double rand = Math.random();
    double tempDist = 0;
    for (Integer i : distribution.keySet()) {
      tempDist += distribution.get(i);
      if (rand * distSum <= tempDist) {
        return i;
      }
    }
    return -1; // error
  }

}
