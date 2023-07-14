package run.acloud.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileUtils {
    public static String readAllLines(String path) throws CocktailException{
        return FileUtils.readAllLines(path, true);
    }

    public static String readAllLines(String path, boolean isThrow) throws CocktailException{
        try {
            if(StringUtils.isBlank(path)) {
                throw new CocktailException("File not found (path is null): " + path, ExceptionType.FileNotFound);
            }
            else {
                Path check = Paths.get(path);
                if (!Files.exists(check)) {
                    throw new CocktailException("File not found : " + path, ExceptionType.FileNotFound);
                }
            }

            Charset charset = Charset.forName("UTF-8");
            return Files.readAllLines(Paths.get(path), charset).stream()
                .collect(Collectors.joining(System.lineSeparator()));
        }
        catch (CocktailException ce) {
            if(isThrow) {
                throw ce;
            }
            else {
                log.warn("File not found : " + path);
            }
        }
        catch (IOException ie) {
            if (isThrow) {
                throw new CocktailException("Failed to read file : " + path, ExceptionType.FileReadFailure);
            }
            else {
                log.warn("Failed to read file: " + path);
            }
        }

        return null;
    }
}
