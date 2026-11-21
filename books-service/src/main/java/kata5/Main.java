package kata5;

import java.util.List;

public class Main {
    static void main(String[] args) {
        String version = args.length > 4 ? args[4] : "v1";
        WebService service = new WebService(
                args[0],
                args[1],
                Integer.parseInt(args[2]),
                args[3],
                version,
                createBooks()
        );
        service.start();
    }

    private static List<Books> createBooks() {
        return List.of(
                new Books("1", "1984", "George Orwell", "1949", Genre.DYSTOPIAN),
                new Books("2", "Dune", "Frank Herbert", "1965", Genre.SCI_FI),
                new Books("3", "The Hobbit", "J.R.R. Tolkien", "1937", Genre.FANTASY),
                new Books("4", "Clean Code", "Robert C. Martin", "2008", Genre.PROGRAMMING),
                new Books("5", "How to Talk to Your Cat About Gun Safety", "Zachary Auburn", "2016", Genre.MEME),
                new Books("6", "Everything I Want to Do Is Illegal", "Joel Salatin", "2007", Genre.MEME),
                new Books("7", "HTML for Babies", "John C. Vanden-Heuvel Sr.", "2011", Genre.MEME),
                new Books("8", "The Hitchhiker's Guide to the Galaxy", "Douglas Adams", "1979", Genre.SCI_FI)
        );
    }
}
