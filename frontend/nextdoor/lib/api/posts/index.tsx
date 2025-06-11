import axiosInstance from "../instance";
export type CreatePostRequest = import("@/types/posts/request").PostCreateRequestDTO;

// 공통 에러 처리 함수
const handleApiError = (error: any, name: string) => {
  console.error(`${name} API 요청 중 오류가 발생했습니다.`, error);
  throw new Error(
    error.response?.data?.message || `${name} API 요청 중 오류가 발생했습니다.`
  );
};

// 게시글 목록 조회
export const GetPostListRequest = (
  userId: string,
  page: number = 0,
  size: number = 10,
  sorted: string = "desc"
) =>
  //파라미터 sorted 넣기
  axiosInstance
    .get(`/api/v1/posts`, {
      params: {
        userId,
        page,
        size,
        sorted: "desc",
      },
    })
    .then((response) => {
      return response.data;
    })
    .catch((error) => {
      return handleApiError(error, "게시글 목록 조회");
    });

// 검색어 자동완성 API
export const getSuggestions = async (prefix: string) => {
  try {
    console.log('자동완성 API 호출:', prefix);
    const response = await axiosInstance.get('/api/v1/posts/search/suggestions', {
      params: { prefix }
    });
    console.log('자동완성 API 응답:', response.data);
    return response.data;
  } catch (error: any) {
    console.error('자동완성 API 에러 상세:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status
    });
    return handleApiError(error, "검색어 자동완성");
  }
};

// 검색 API
export const searchPosts = async ({
  keyword,
  page = 0,
  size = 10,
  sort = 'createdAt',
  direction = 'DESC'
}: {
  keyword: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}) => {
  try {
    const response = await axiosInstance.get('/api/v1/posts/search', {
      params: {
        keyword,
        page,
        size,
        sort,
        direction
      }
    });
      return response.data;
    } catch (error) {
    return handleApiError(error, "게시글 검색");
  }
};

// 좋아요한 게시글 목록 조회
export const GetPostLikeListRequest = (
  userId: string,
  page: number = 0,
  size: number = 10,
  sorted: string = "desc"
) =>
  axiosInstance
    .get(`/api/v1/posts/liked`, {
      params: {
        userId,
        page,
        size,
        sorted,
      },
    })
    .then((response) => {
      return response.data;
    })
    .catch((error) => {
      return handleApiError(error, "좋아요한 게시글 목록 조회");
    });

// 게시글 좋아요 여부 조회
export const GetPostLikeRequest = (postId: string) =>
  axiosInstance
    .get(`/api/v1/posts/${postId}/like`)
    .then((response) => {
      return response.data;
    })
    .catch((error) => {
      return handleApiError(error, "게시글 좋아요 여부 조회");
    });


// 게시글 좋아요 요청 api
export const PostLikeRequest = (postId: string) =>
  axiosInstance
    .post(`/api/v1/posts/${postId}/like`)
    .then((response) => {
      return response.data;
    })
    .catch((error) => {
      return handleApiError(error, "게시글 좋아요 요청");
    });


// 게시글 좋아요 취소 요청 api
export const PostLikeDeleteRequest = (postId: string) =>
  axiosInstance
    .delete(`/api/v1/posts/${postId}/like`)
    .then((response) => {
      return response.data;
    })
    .catch((error) => {
      return handleApiError(error, "게시글 좋아요 취소 요청");
    });

