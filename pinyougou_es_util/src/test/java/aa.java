import java.util.Arrays;

public class aa {

    public static void main(String[] args) {

        System.out.println(Arrays.asList("Hello", "World", "How", "Are", "You")
                .stream()
                .map( s -> "_" + s + "_")
                .reduce( (s1, s2) -> s1 + "," + s2)
                .get());

    }

}
