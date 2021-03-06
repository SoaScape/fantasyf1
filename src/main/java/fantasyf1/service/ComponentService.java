package fantasyf1.service;

import java.util.List;
import java.util.Map;

import fantasyf1.domain.Car;
import fantasyf1.domain.Driver;
import fantasyf1.domain.Engine;

public interface ComponentService {
	public Map<String, Long> getAllDriverPoints();
	
	public Map<String, Long> getAllCarPoints();
	
	public Map<String, Long> getAllEnginePoints();
	
	public Driver findDriverByName(final String name);

	public Driver findDriverByNumber(final int number);

	public List<Driver> findDriversByStandin(final boolean standIn);

	public List<Driver> findDriversByCar(final Car car);

	public List<Driver> findDriversByCarAndStandin(final Car car,
			final boolean standIn);
	
	public void saveDrivers(List<Driver> drivers, final boolean merge);

	public Car findCarById(final Integer id);
	
	public Car findCarByName(final String name);

	public List<Car> findCarsByEngine(final Engine engine);

	public Engine findEngineByName(final String name);

	public List<Driver> findAllDrivers();

	public List<Car> findAllCars();
	
	public void saveCars(List<Car> cars, final boolean merge);

	public List<Engine> findAllEngines();
	
	public void saveEngines(List<Engine> engines, final boolean merge);

	public void saveDriver(final Driver driver);

	public void saveCar(final Car car);

	public void saveEngine(final Engine engine);
}
