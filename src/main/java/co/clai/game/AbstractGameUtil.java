package co.clai.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

public class AbstractGameUtil {

	public static List<AbstractGame> getAllGames() {

		List<AbstractGame> retList = new ArrayList<>();

		Reflections reflections = new Reflections("co.clai.game");

		Set<Class<? extends AbstractGame>> allClasses = reflections.getSubTypesOf(AbstractGame.class);

		for (Class<? extends AbstractGame> c : allClasses) {

			try {
				AbstractGame m = c.newInstance();

				retList.add(m);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

		return retList;
	}

}
