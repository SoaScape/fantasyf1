package fantasyf1.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fantasyf1.domain.Car;
import fantasyf1.domain.Driver;
import fantasyf1.domain.Engine;
import fantasyf1.domain.EventResult;
import fantasyf1.domain.PointScorer;
import fantasyf1.domain.Position;
import fantasyf1.domain.Rules;
import fantasyf1.domain.Team;
import fantasyf1.domain.TheoreticalTeam;
import fantasyf1.service.ComponentService;
import fantasyf1.service.EventService;
import fantasyf1.service.LeagueService;
import fantasyf1.service.TeamService;

@Service
public class LeagueServiceImpl implements LeagueService {

	private static final Logger LOG = Logger.getLogger(LeagueServiceImpl.class);

	@Value("#{new java.text.SimpleDateFormat(\"${dateFormat}\").parse(\"${season-start-date-time}\")}")
	private Date seasonStartDateTime;

	@Value("${refresh-results-on-page-load}")
	private boolean refreshResultsOnPageLoad;

	@Value("${best-theoretical-team-name}")
	private String bestTheoreticalTeamName;

	@Autowired
	TeamService teamService;

	@Autowired
	Rules rules;

	@Autowired
	ComponentService componentService;

	@Autowired
	EventService eventService;

	@Override
	public List<Team> calculateLeagueStandings() {
		final List<Team> teams = teamService.findAll();

		if (refreshResultsOnPageLoad && eventService.checkForNewResults(true)) {
			calculateAllResults(teams);
		}

		Collections.sort(teams);
		return teams;
	}

	@Override
	public synchronized void recalculateAllResults() {
		calculateAllResults(teamService.findAll());
	}

	private synchronized void calculateAllResults(final List<Team> teams) {
		LOG.info("Recalculating scores...");
		final List<EventResult> results = eventService.getSeasonResults();
		resetAllScores(teams);
		for (final EventResult result : results) {
			calculateResult(result, teams);
		}
		LOG.info("Scores recalculated.");
	}

	private void calculateBestTheoreticalTeam(final EventResult result) {
		final List<Driver> allDrivers = componentService
				.findDriversByStandin(false);
		final List<Car> cars = componentService.findAllCars();
		final List<Engine> engines = componentService.findAllEngines();

		TheoreticalTeam bestTeamForRound;

		if(result.getBestTheoreticalTeam() != null) {
			bestTeamForRound = result.getBestTheoreticalTeam();
			bestTeamForRound.reset();
		} else {
			bestTeamForRound = new TheoreticalTeam();
		}

		bestTeamForRound.setName("Best Theoretical Team For Round "
				+ result.getRound());

		result.setBestTheoreticalTeam(bestTeamForRound);

		TheoreticalTeam bestOverallTeam;
		final TheoreticalTeam res = teamService
				.findTheoreticalTeamByName(bestTheoreticalTeamName);

		if (res == null) {
			bestOverallTeam = new TheoreticalTeam();
			bestOverallTeam.setName(bestTheoreticalTeamName);
		} else {
			bestOverallTeam = res;
		}

		long roundHighScore = 0;
		long totalHighScore = bestOverallTeam.getPoints() == null ? 0
				: bestOverallTeam.getPoints();

		for (final Driver driver1 : allDrivers) {
			final Team team = new Team();
			for (final Driver driver2 : allDrivers) {
				for (final Driver driver3 : allDrivers) {
					if (driver1.getNumber() != driver2.getNumber() && driver1.getNumber() != driver3.getNumber() && driver2.getNumber() != driver3.getNumber()) {
						team.setDrivers(new ArrayList<Driver>());
						team.getDrivers().add(driver1);
						team.getDrivers().add(driver2);
						team.getDrivers().add(driver3);
						for (final Car car : cars) {
							team.setCar(car);
							for (final Engine engine : engines) {
								team.setEngine(engine);
								try {
									teamService.validateTeamComponents(team);
									final long roundScore = calculateRoundScore(
											result.getRound(), team);
									final long totalScore = calculateTotalScore(team);

									if (roundScore > roundHighScore) {
										roundHighScore = roundScore;
										bestTeamForRound.setComponents(team);

										bestTeamForRound
												.getDrivers()
												.get(0)
												.setPoints(
														(long) team
																.getDrivers()
																.get(0)
																.getPointsPerEvent()
																.get(result.getRound()));
										bestTeamForRound
												.getDrivers()
												.get(1)
												.setPoints(
														(long) team
																.getDrivers()
																.get(1)
																.getPointsPerEvent()
																.get(result.getRound()));

										bestTeamForRound
										.getDrivers()
										.get(2)
										.setPoints(
												(long) team
														.getDrivers()
														.get(2)
														.getPointsPerEvent()
														.get(result.getRound()));
										bestTeamForRound.getCar().setPoints(
												(long) team.getCar()
														.getPointsPerEvent()
														.get(result.getRound()));
										bestTeamForRound.getEngine().setPoints(
												(long) team.getEngine()
														.getPointsPerEvent()
														.get(result.getRound()));

										bestTeamForRound.setPoints(roundScore);
									}
									if (totalScore > totalHighScore) {
										totalHighScore = totalScore;
										bestOverallTeam.setComponents(team);
										bestOverallTeam.setPoints(totalScore);
									}
								} catch (final ValidationException e) {
									continue;
								}
							}
						}
					}
				}
			}
		}
		teamService.saveTheoreticalTeam(bestOverallTeam);
		eventService.save(result);
	}

	private long calculateRoundScore(final int round, final Team team) {
		long score = 0;
		for(final Driver driver : team.getDrivers()) {
			score += driver.getPointsPerEvent().get(round);
		}
		score += team.getCar().getPointsPerEvent().get(round)
			  + team.getEngine().getPointsPerEvent().get(round);
		return score;
	}

	private long calculateTotalScore(final Team team) {
		long score = 0;
		for(final Driver driver : team.getDrivers()) {
			score += driver.getTotalPoints();
		}
		score +=  team.getCar().getTotalPoints()
			  + team.getEngine().getTotalPoints();
		return score;
	}

	private void resetAllScores(final List<Team> teams) {
		for (final Team team : teams) {
			resetPointsScorer(team);
			teamService.saveTeamNoValidation(team);
		}
		final List<Driver> drivers = componentService
				.findAllDrivers();
		for (final Driver driver : drivers) {
			driver.setFastestLaps(0);
			resetPointsScorer(driver);
			componentService.saveDriver(driver);
		}

		final List<Car> cars = componentService.findAllCars();
		for (final Car car : cars) {
			car.setBothCarsFinishBonuses(0);
			resetPointsScorer(car);
			componentService.saveCar(car);
		}

		final List<Engine> engines = componentService.findAllEngines();
		for (final Engine engine : engines) {
			resetPointsScorer(engine);
			componentService.saveEngine(engine);
		}
		teamService.resetBestTheoreticalTeam();
	}

	private void resetPointsScorer(final PointScorer scorer) {
		scorer.setTotalPoints(0);
		scorer.setPointsPerEvent(new LinkedHashMap<Integer, Integer>());
	}

	private synchronized void calculateResult(final EventResult result,
			final List<Team> teams) {
		for (final Driver driver : componentService.findDriversByStandin(false)) {
			int points = 0;
			Position pos = result.getQualifyingOrder().get(driver.getName());
			if (pos != null) {
				if (pos.isClassified()) {
					points += rules.getDriverQualPoints().get(pos.getPosition());
				}
			} else {
				// Is there a stand in driver?
				final List<Driver> standIns = componentService
						.findDriversByCarAndStandin(driver.getCar(), true);
				for (final Driver standInDriver : standIns) {
					pos = result.getQualifyingOrder().get(
							standInDriver.getName());
					if (pos != null) {
						if (pos.isClassified()) {
							points += rules.getDriverQualPoints().get(pos.getPosition());
						}
						result.addRemark(driver.getName()
								+ " scores qualifying points from stand-in driver "
								+ standInDriver.getName());
						eventService.save(result);
						break;
					}
				}
			}

			if (result.isRaceComplete()) {
				pos = result.getRaceOrder().get(driver.getName());
				if (pos != null) {
					if (pos.isClassified()) {
						points += rules.getDriverRacePoints().get(pos.getPosition());
					}
					if (driver.equals(result.getFastestLapDriver())) {
						points += rules.getFastestLapBonus();
						driver.setFastestLaps(driver.getFastestLaps() + 1);
					}
				} else {
					// Is there a stand in driver?
					final List<Driver> standIns = componentService
							.findDriversByCarAndStandin(driver.getCar(), true);
					for (final Driver standInDriver : standIns) {
						pos = result.getRaceOrder()
								.get(standInDriver.getName());
						if (pos != null) {
							if (pos.isClassified()) {
								points += rules.getDriverRacePoints().get(pos
										.getPosition());
							}
							if (standInDriver.equals(result
									.getFastestLapDriver())) {
								points += rules.getFastestLapBonus();
							}
							result.addRemark(driver.getName()
									+ " scores race points from stand-in driver "
									+ standInDriver.getName());
							eventService.save(result);
							break;
						}
					}
				}
			}
			driver.getPointsPerEvent().put(result.getRound(), points);
			driver.setTotalPoints(driver.getTotalPoints() + points);
			componentService.saveDriver(driver);
		}

		for (final Car car : componentService.findAllCars()) {
			int points = 0;
			final List<Driver> carDrivers = componentService
					.findDriversByCar(car);

			int numCarsParticipated = 0;
			int numCarsFinished = 0;
			for (final Driver driver : carDrivers) {
				Position pos = result.getQualifyingOrder()
						.get(driver.getName());
				if (pos != null) {
					if (pos.isClassified()) {
						points += rules.getCarQualPoints().get(pos.getPosition());
					}
				}

				if (result.isRaceComplete()) {
					pos = result.getRaceOrder().get(driver.getName());
					if (pos != null) {
						numCarsParticipated++;
						if (pos.isClassified()) {
							points += rules.getCarRacePoints().get(pos.getPosition());
							numCarsFinished++;
						}
					}
				}
			}
			if (numCarsFinished == 2) {
				points += rules.getBothCarsFinishedBonus();
				car.setBothCarsFinishBonuses(car.getBothCarsFinishBonuses() + 1);
			}
			if (result.isRaceComplete() && numCarsParticipated == 0) {
				result.addRemark(car.getName()
						+ " did not participate in the race.");
			}
			car.getPointsPerEvent().put(result.getRound(), points);
			car.setTotalPoints(car.getTotalPoints() + points);
			componentService.saveCar(car);
		}

		for (final Engine engine : componentService.findAllEngines()) {
			final List<Car> carsUsingEngine = componentService
					.findCarsByEngine(engine);
			final List<Driver> engineDrivers = new ArrayList<Driver>();
			for (final Car car : carsUsingEngine) {
				final List<Driver> drivers = componentService
						.findDriversByCar(car);
				for (final Driver driver : drivers) {
					engineDrivers.add(driver);
				}
			}

			int points = 0;

			for (final Driver driver : engineDrivers) {
				Position pos = result.getQualifyingOrder()
						.get(driver.getName());
				if (pos != null) {
					if (pos.isClassified()) {
						points += rules.getEngineQualPoints().get(pos.getPosition());
					}
				}

				if (result.isRaceComplete()) {
					pos = result.getRaceOrder().get(driver.getName());
					if (pos != null) {
						if (pos.isClassified()) {
							points += rules.getEngineRacePoints().get(pos.getPosition());
						}
					}
				}
			}
			engine.getPointsPerEvent().put(result.getRound(), points);
			engine.setTotalPoints(engine.getTotalPoints() + points);
			componentService.saveEngine(engine);
		}

		final Iterator<Team> teamItr = teamService.findAll().iterator();
		while (teamItr.hasNext()) {
			final Team team = teamItr.next();
			if (!team.getName().equals(bestTheoreticalTeamName)) {
				int points = 0;
				for (final Driver driver : team.getDrivers()) {
					points += driver.getPointsPerEvent().get(result.getRound());
				}
				points += team.getCar().getPointsPerEvent()
						.get(result.getRound());
				points += team.getEngine().getPointsPerEvent()
						.get(result.getRound());
				team.getPointsPerEvent().put(result.getRound(), points);
				team.setTotalPoints(team.getTotalPoints() + points);
				teamService.saveTeamNoValidation(team);
			}
		}
		calculateBestTheoreticalTeam(result);
	}

	@Override
	public Rules getRules() {
		return rules;
	}

	@Override
	public boolean seasonStarted() {
		return new Date().after(seasonStartDateTime);
	}

	@Override
	public Date getSeasonStartDate() {
		return seasonStartDateTime;
	}
}
