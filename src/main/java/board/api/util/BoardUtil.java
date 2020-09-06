package board.api.util;

import board.api.model.Board;
import board.api.model.Feature;
import board.api.model.Spec;
import board.api.model.Stance;
import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.repository.query.SpelQueryContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@PropertySource("classpath:board.properties")
public class BoardUtil {

    private final Faker faker = new Faker();
    private final Random random = new Random();
    @Value("${board.size}")
    private List<String> sizes;
    @Value("${board.riding.level}")
    private List<String> ridingLevels;
    @Value("${board.bend}")
    private List<String> bends;
    @Value("${board.shape}")
    private List<String> shapes;
    @Value("${board.terrain}")
    private List<String> terrains;
    @Value("${board.stance.min}")
    private List<String> stanceMin;
    @Value("${board.stance.max}")
    private List<String> stanceMax;
    @Value("${board.stance.setBack}")
    private List<String> stanceSetBack;

    public Board getRandomBoard() {
        Board board = new Board();
        board.setId(UUID.randomUUID().toString());
        board.setYear(getIntInRange(2010, 2020));
        board.setName(faker.animal().name());
        board.setCode(faker.code().imei());
        board.setSize(getRandom(sizes));
        board.setSpec(getRandomSpec(board.getSize()));
        board.setFeatures(getRandomFeatures());
        board.setTerrain(getRandom(terrains));
        board.setRidingLevel(getRandom(ridingLevels));
        board.setBend(getRandom(bends));
        board.setShape(getRandom(shapes));
        board.setDetails(faker.shakespeare().romeoAndJulietQuote());
        return board;
    }

    private Spec getRandomSpec(String size) {
        Spec spec = new Spec();
        spec.setContactLength(getContactLength(size));
        spec.setSideCut(getDoubleInRange(7.0, 9.0));
        spec.setWaistWidth(getDoubleInRange(17.0, 24.5));
        spec.setNoseWidth(getSimpleDouble(getBoardLength(size)/5.13));
        spec.setTailWidth(getSimpleDouble(spec.getNoseWidth()/1.0555));
        spec.setWeightRange(getWeightRange(size));
        spec.setFlex(getIntInRange(1, 10));
        spec.setStance(getRandomStance());
        return spec;
    }

    private int getWeightRange(String size) {
        return getBoardLength(size) - getIntInRange(70, 84);
    }

    private int getBoardLength(String size) {
        return Integer.parseInt(size.substring(0,3));
    }

    private int getContactLength(String size) {
        return Integer.parseInt(size.substring(0,3)) - 40;
    }

    private double getDoubleInRange(double min, double max) {
        return getSimpleDouble(min + (max - min) * random.nextDouble());
    }

    private double getSimpleDouble(double value) {
        return Math.floor(value * 10) / 10;
    }

    private int getIntInRange(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private Stance getRandomStance() {
        Stance stance = new Stance();
        stance.setMin(Double.parseDouble(getRandom(stanceMin)));
        stance.setMax(Double.parseDouble(getRandom(stanceMax)));
        stance.setSetBack(Double.parseDouble(getRandom(stanceSetBack)));
        return stance;
    }

    private List<Feature> getRandomFeatures() {
        return Stream
                .generate(this::getRandomFeature)
                .limit(random.nextInt(4))
                .collect(Collectors.toList());
    }

    private Feature getRandomFeature() {
        Feature feature = new Feature();
        feature.setType(faker.witcher().location());
        feature.setName(faker.witcher().monster());
        feature.setDescription(faker.dune().quote());
        return feature;
    }

    private String getRandom(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }

}
