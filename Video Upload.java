@SpringBootApplication
public class VideoUploadApp {
    public static void main(String[] args) {
        SpringApplication.run(VideoUploadApp.class, args);
    }
}

@RestController
@RequestMapping("/videos")
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ResponseEntity<VideoDTO> uploadVideo(@RequestParam("file") MultipartFile file, @RequestParam("title") String title) {
        Video video = videoService.uploadVideo(file, title);
        return ResponseEntity.ok(VideoDTO.fromVideo(video));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideo(@PathVariable Long id) {
        Video video = videoService.getVideo(id);
        return ResponseEntity.ok(VideoDTO.fromVideo(video));
    }
}

@Service
public class VideoService {
    private final VideoRepository videoRepository;
    private final StorageService storageService;

    public VideoService(VideoRepository videoRepository, StorageService storageService) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
    }

    public Video uploadVideo(MultipartFile file, String title) {
        String fileName = storageService.storeFile(file);
        Video video = new Video(title, fileName);
        return videoRepository.save(video);
    }

    public Video getVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
    }
}

@Entity
@Data
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String fileName;
    private LocalDateTime uploadedAt;

    public Video(String title, String fileName) {
        this.title = title;
        this.fileName = fileName;
        this.uploadedAt = LocalDateTime.now();
    }
}

@Data
public class VideoDTO {
    private Long id;
    private String title;
    private String fileName;
    private LocalDateTime uploadedAt;

    public static VideoDTO fromVideo(Video video) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setFileName(video.getFileName());
        dto.setUploadedAt(video.getUploadedAt());
        return dto;
    }
}

@Service
public class StorageService {
    private final Path uploadDir = Paths.get("uploads");

    public String storeFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error storing file", e);
        }
    }

    public Path getFilePath(String fileName) {
        return uploadDir.resolve(fileName);
    }
}

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Add additional exception handlers as needed
}

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
