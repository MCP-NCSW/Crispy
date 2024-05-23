package com.mcp.crispy.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatRoomDto {
    private Integer chatRoomNo;
    private String chatRoomTitle;
    private Integer chatRoomStat;
    private Date createDt;
    private Integer creator;
    private Date modifyDt;
    private Integer modifier;
}
