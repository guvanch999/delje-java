package tm.delje.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import tm.delje.shop.cli.CommandLineTasks;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DeljeApplication {

    public static void main(String[] args) {
        if (args.length > 0 && CommandLineTasks.isTask(args[0])) {
            System.exit(CommandLineTasks.run(args));
        }
        SpringApplication.run(DeljeApplication.class, args);
    }
}
