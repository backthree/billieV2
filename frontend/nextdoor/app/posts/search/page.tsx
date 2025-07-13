"use client"

import PostListItem from "@/components/(tabs)/home/PostListItem";
import { Loader2 } from "lucide-react";
import { GetPostListRequest } from "@/lib/api/posts";
import Header from "@/components/pays/common/Header";
import Link from "next/link";
import { useState, useEffect } from "react";
import { PostListItemDto } from "@/types/posts/response";

export default function HomePage() {
    const [postList, setPostList] = useState<PostListItemDto[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        setIsLoading(true);
        GetPostListRequest().then((res) => {
            setPostList(res.content);
            setIsLoading(false);
        });
    }, []);

    return (
        <div className="flex flex-col">
            <Header txt="물품 검색" />
            <main className="p-4 mb-20">
                {isLoading ? (
                    <div className="flex justify-center items-center h-full">
                        <Loader2 className="w-6 h-6 animate-spin" />
                    </div>
                ) : (
                    <div className="flex flex-col gap-3 text-gray900">
                        {postList.map((post: PostListItemDto) => (
                            <Link href={`/posts/${post.postId}`} key={post.postId}>
                                <PostListItem key={post.postId} post={post} />
                            </Link>
                        ))}
                    </div>
                )}
            </main>
        </div>
    );
} 