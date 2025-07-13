"use client"

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { CreatePostRequest } from "@/lib/api/posts";
import { postCreateRequest } from "@/lib/api/posts/request/index";
import Header from "@/components/pays/common/Header";
import { Loader2 } from "lucide-react";

export default function WritePage() {
    const router = useRouter();
    const [isLoading, setIsLoading] = useState(false);
    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");
    const [price, setPrice] = useState("");

    const onSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        const data: CreatePostRequest = {
            title,
            content,
            category: "기타", // 임시 값
            rentalFee: parseInt(price),
            deposit: 0, // 임시 값
            preferredLocation: { latitude: 0, longitude: 0 }, // 임시 값
            address: "", // 임시 값
        };
        postCreateRequest(data, null, null).then(() => {
            router.push("/posts");
        });
    };

    return (
        <div className="flex flex-col">
            <Header txt="물품 등록" />
            <main className="p-4 mb-20">
                <form onSubmit={onSubmit} className="flex flex-col gap-4">
                    <input
                        type="text"
                        placeholder="제목"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        className="p-2 border rounded"
                    />
                    <textarea
                        placeholder="내용"
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        className="p-2 border rounded h-32"
                    />
                    <input
                        type="number"
                        placeholder="가격"
                        value={price}
                        onChange={(e) => setPrice(e.target.value)}
                        className="p-2 border rounded"
                    />
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="p-2 bg-blue-500 text-white rounded disabled:bg-blue-300"
                    >
                        {isLoading ? (
                            <Loader2 className="w-6 h-6 animate-spin mx-auto" />
                        ) : (
                            "등록"
                        )}
                    </button>
                </form>
            </main>
        </div>
    );
} 