import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Random;

public class MmapExperiment {

    static final int FILE_SIZE = 100 * 1024 * 1024; // 100 МБ в байтах
    static final int PAGE_SIZE = 4096;
    static final int NUM_PAGES = 1000;
    static final int RUNS = 10;

    public static void main(String[] args) throws Exception {
        runTest();
    }

    static void runTest() throws IOException {
        long totalFirst = 0, totalSecond = 0;
        Random rnd = new Random();

        for (int run = 0; run < RUNS; run++) {
            Path file = Files.createTempFile("mmap_test", ".bin");
            try {
                createRandomFile(file);

                try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
                    MappedByteBuffer mmap = ch.map(
                            FileChannel.MapMode.READ_ONLY,
                            0,
                            ch.size()
                    );

                    int maxPage = FILE_SIZE / PAGE_SIZE;
                    int[] pages = new int[NUM_PAGES];

                    for (int i = 0; i < NUM_PAGES; i++) {
                        pages[i] = rnd.nextInt(maxPage);
                    }

                    totalFirst += accessPages(mmap, pages);
                    totalSecond += accessPages(mmap, pages);
                }
            } finally {
                Files.deleteIfExists(file);
            }
        }

        System.out.printf("Среднее время первого прохода: %.5f ms%n", totalFirst / (double) RUNS / 1_000_000);
        System.out.printf("Среднее время второго прохода: %.5f ms%n", totalSecond / (double) RUNS / 1_000_000);
    }

    static void createRandomFile(Path file) throws IOException {
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
            int written = 0;
            Random rnd = new Random();

            while (written < FILE_SIZE) {
                rnd.nextBytes(buf.array());
                buf.clear();
                ch.write(buf);
                written += buf.capacity();
            }
        }
    }

    static long accessPages(MappedByteBuffer mmap, int[] pages) {
        long start = System.nanoTime();
        byte sum = 0;
        for (int page : pages) {
            sum ^= mmap.get(page * PAGE_SIZE);
        }
        return System.nanoTime() - start;
    }
}