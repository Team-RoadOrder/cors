package dev.gmpark.cors.vos;


import lombok.Getter;

@Getter
public class PageVo {
    private final  int rowCount = 8;
    private final  int anchorCount = 5;
    private final  int minPage = 1;
    private final int maxPage ;
    private final int startPage;
    private final int endPage;
    private final  int totalCount;
    private final int requestPage;
    private final int dbOffset;

    public PageVo(int requestPage, int totalCount) {
        this.requestPage = requestPage;
        this.totalCount = totalCount;
        this.maxPage = totalCount == 0 ? 1 : totalCount / this.rowCount + (totalCount % this.rowCount == 0 ? 0 : 1 );
        this.startPage = (requestPage / this.anchorCount ) * this.anchorCount  + 1;
        this.endPage = Math.min( this.maxPage, this.startPage + (this.anchorCount -1) );
        this.dbOffset = (this.requestPage - 1) * this.rowCount;
    }

}
