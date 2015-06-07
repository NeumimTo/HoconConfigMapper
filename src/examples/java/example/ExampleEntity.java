package example;

/**
 * Created by NeumimTo on 5.6.2015.
 */

import cz.neumimto.configuration.ConfigValue;
import cz.neumimto.configuration.Entity;

@Entity(directoryPath = "{WorkingDir}")
public class ExampleEntity {

    public ExampleEntity(String name, double someval) {
        this.name = name;
        this.someval = someval;
    }

    /**
     * id is used as filename
     */
    @Entity.Id
    private String name;


    @ConfigValue
    private double someval;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSomeval() {
        return someval;
    }

    public void setSomeval(double someval) {
        this.someval = someval;
    }
}
