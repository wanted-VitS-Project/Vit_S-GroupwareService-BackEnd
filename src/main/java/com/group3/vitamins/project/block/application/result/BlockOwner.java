package com.group3.vitamins.project.block.application.result;

/** 블록 담당자. 미지정이면 이 객체 자체가 null 이다. */
public record BlockOwner(String userId, String name) {
}