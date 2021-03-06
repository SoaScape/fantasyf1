package fantasyf1.domain;

import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public final class Rules {
	@Value("${budget}")
	private int budget;
	@Value("${num-drivers-per-team}")
	private String numDriversPerTeam;
	@Value("${fastest-lap-bonus}")
	private Integer fastestLapBonus;
	@Value("${both-cars-finish-bonus}")
	private Integer bothCarsFinishedBonus;

	@Value("#{${driver-qual-points}}")
	private Map<Integer, Integer> driverQualPoints;

	@Value("#{${driver-race-points}}")
	private Map<Integer, Integer> driverRacePoints;

	@Value("#{${car-qual-points}}")
	private Map<Integer, Integer> carQualPoints;

	@Value("#{${car-race-points}}")
	private Map<Integer, Integer> carRacePoints;

	@Value("#{${engine-qual-points}}")
	private Map<Integer, Integer> engineQualPoints;

	@Value("#{${engine-race-points}}")
	private Map<Integer, Integer> engineRacePoints;
}
