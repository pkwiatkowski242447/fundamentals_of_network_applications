package pl.pas.gr3.mvc.controller.movie;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.pas.gr3.dto.input.MovieInputDTO;
import pl.pas.gr3.mvc.exceptions.beans.movies.MovieCreateException;

import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor
@RequestScoped
@Named
public class CreateMovieBean implements Serializable {

    private MovieInputDTO movieInputDTO;
    private String message;

    @Inject
    private MovieControllerBean movieControllerBean;

    @PostConstruct
    public void beanInit() {
        movieInputDTO = new MovieInputDTO();
    }

    // Methods

    public String createMovie() {
        try {
            movieControllerBean.createMovie(movieInputDTO);
            return "movieCreatedSuccessfully";
        } catch (MovieCreateException exception) {
            message = exception.getMessage();
            return null;
        }
    }
}
