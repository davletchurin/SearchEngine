package searchengine.util;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Page {
    private Long siteId;
    private String path;
    private Integer code;
    private String content;
    private List<Page> children; // TODO: нужен или нет?
}
