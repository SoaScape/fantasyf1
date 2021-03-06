package fantasyf1.domain;

import java.util.Map;

public interface PointScorer {
	String getName();
	long getTotalPoints();

	Map<Integer, Integer> getPointsPerEvent();

	void setTotalPoints(final long totalPoints);

	void setPointsPerEvent(final Map<Integer, Integer> pointsPerEvent);
}
