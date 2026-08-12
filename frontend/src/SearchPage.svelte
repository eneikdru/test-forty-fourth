<script>
  // Props in Svelte 5
  let { onSelectMaterial, onNavigateToCatalog, onNavigateToCreate } = $props();

  // Svelte 5 State Runes
  let searchQuery = $state("Influenza");
  let selectedCategory = $state("");
  let selectedStatus = $state("");
  let currentPage = $state(1);
  let totalItems = $state(0);
  let totalPages = $state(1);
  let itemsPerPage = 10;
  let items = $state([]);
  let isLoading = $state(false);
  let errorMsg = $state("");

  // Categories list based on mockup
  const CATEGORIES = [
    { name: "All Protocols", value: "", icon: "database" },
    { name: "Respiratory", value: "Respiratory", icon: "air" },
    { name: "Vector-borne", value: "Vector-borne", icon: "bug_report" },
    { name: "Enteric", value: "Enteric", icon: "water_drop" },
    { name: "Zoonotic", value: "Zoonotic", icon: "pets" }
  ];

  // Load materials from API or fallback to localStorage / mock
  async function fetchResults() {
    isLoading = true;
    errorMsg = "";

    const queryParams = new URLSearchParams();
    if (searchQuery.trim()) {
      queryParams.append("q", searchQuery.trim());
    }
    if (selectedCategory) {
      queryParams.append("category", selectedCategory);
    }
    if (selectedStatus) {
      queryParams.append("status", selectedStatus);
    }
    queryParams.append("page", currentPage.toString());
    queryParams.append("limit", itemsPerPage.toString());

    try {
      const response = await fetch(`/api/materials/search?${queryParams.toString()}`);
      if (response.ok) {
        const data = await response.json();
        items = data.items || [];
        totalItems = data.total || 0;
        totalPages = data.pages || 1;
      } else {
        throw new Error("API response error");
      }
    } catch (e) {
      console.warn("API request failed, falling back to local search.", e);
      // Local fallback implementation
      fallbackLocalSearch();
    } finally {
      isLoading = false;
    }
  }

  function fallbackLocalSearch() {
    let allMaterials = [];
    try {
      const stored = localStorage.getItem("epitrack_materials");
      if (stored) {
        allMaterials = JSON.parse(stored);
      }
    } catch (e) {
      console.error(e);
    }

    if (allMaterials.length === 0) {
      // Default mock data aligned with App.svelte
      allMaterials = [
        {
          id: "EP-2023-A01",
          title: "SARS-CoV-2 Batch A",
          content: "SARS-CoV-2 high concentration viral strain for reference standard calibration.",
          category: "Viral Strains",
          updated_at: "2023-10-12T14:30:00Z",
          status: "Analyzed"
        },
        {
          id: "EP-2023-A02",
          title: "Influenza A H1N1 Ref",
          content: "Standard operating procedure reference standard for Influenza A H1N1.",
          category: "Viral Strains",
          updated_at: "2023-10-14T09:15:00Z",
          status: "Pending"
        },
        {
          id: "EP-2023-B01",
          title: "E. Coli Strain K12",
          content: "Laboratory-safe model strain E. coli K12 for genetic material comparison.",
          category: "Bacterial Cultures",
          updated_at: "2023-09-28T16:45:00Z",
          status: "Archived"
        },
        {
          id: "EP-2023-A03",
          title: "Unknown Isolate 44X",
          content: "Atypical viral isolate found in sample 44X with potential respiratory pathogenicity.",
          category: "Viral Strains",
          updated_at: "2023-10-20T11:00:00Z",
          status: "Urgent"
        },
        {
          id: "EP-2023-C01",
          title: "Mab-IgG-12",
          content: "Monoclonal antibody IgG batch 12 designed for epidemiological assays.",
          category: "Antibodies",
          updated_at: "2023-10-05T08:20:00Z",
          status: "Analyzed"
        }
      ];
    }

    // Filter local data
    let filtered = allMaterials.filter(m => {
      const query = searchQuery.trim().toLowerCase();
      const matchesSearch = !query ||
        m.title.toLowerCase().includes(query) ||
        m.id.toString().toLowerCase().includes(query) ||
        (m.content && m.content.toLowerCase().includes(query));

      const matchesCategory = !selectedCategory ||
        m.category.toLowerCase().includes(selectedCategory.toLowerCase()) ||
        selectedCategory.toLowerCase().includes(m.category.toLowerCase());

      const matchesStatus = !selectedStatus || m.status === selectedStatus;

      return matchesSearch && matchesCategory && matchesStatus;
    });

    totalItems = filtered.length;
    totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
    const offset = (currentPage - 1) * itemsPerPage;
    items = filtered.slice(offset, offset + itemsPerPage);
  }

  // Reactive trigger on filter state changes
  $effect(() => {
    fetchResults();
  });

  function selectCategory(catVal) {
    selectedCategory = catVal;
    currentPage = 1;
  }

  function toggleQuickFilter(catVal) {
    if (selectedCategory === catVal) {
      selectedCategory = "";
    } else {
      selectedCategory = catVal;
    }
    currentPage = 1;
  }

  function toggleActiveFilter() {
    if (selectedStatus === "Analyzed") {
      selectedStatus = "";
    } else {
      selectedStatus = "Analyzed";
    }
    currentPage = 1;
  }

  function handleSearchInput(e) {
    searchQuery = e.target.value;
    currentPage = 1;
  }

  function handlePageChange(pageNum) {
    if (pageNum >= 1 && pageNum <= totalPages) {
      currentPage = pageNum;
    }
  }

  function formatDate(isoString) {
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString("en-US", {
        month: "short",
        day: "2-digit",
        year: "numeric"
      });
    } catch (e) {
      return isoString || "Oct 12, 2023";
    }
  }

  // Quick helper to determine badge style matching mockup
  function getBadgeClasses(status) {
    if (status === "Analyzed" || status === "Active") {
      return "bg-[#CCFBF1] text-[#134E4A] border-[#5EEAD4]";
    } else if (status === "Urgent") {
      return "bg-[#FEE2E2] text-[#991B1B] border-[#FCA5A5]";
    } else if (status === "Pending") {
      return "bg-[#FEF3C7] text-[#92400E] border-[#FCD34D]";
    } else {
      return "bg-[#F3F4F6] text-[#374151] border-[#D1D5DB]";
    }
  }

  // Left highlight bar classes matching mockup
  function getLeftBarClasses(status) {
    if (status === "Analyzed" || status === "Active") {
      return "bg-secondary";
    } else if (status === "Urgent") {
      return "bg-[#BA1A1A]";
    } else {
      return "bg-outline-variant";
    }
  }
</script>

<div class="bg-surface text-on-surface font-body-md min-h-screen flex flex-col md:flex-row w-full">
  <!-- Navigation Drawer (Desktop) -->
  <aside class="hidden md:flex flex-col h-screen py-stack-lg bg-surface-container-lowest dark:bg-surface-dim text-secondary dark:text-secondary-fixed font-label-md text-label-md w-64 fixed left-0 top-0 border-r border-outline-variant z-40">
    <div class="px-margin-desktop mb-stack-lg">
      <span class="font-headline-sm text-headline-sm text-primary font-bold">EpiProtocol</span>
    </div>
    <div class="px-margin-desktop mb-stack-sm font-headline-sm text-headline-sm text-on-surface">Categories</div>
    <nav class="flex-1 overflow-y-auto">
      <ul class="space-y-stack-xs">
        {#each CATEGORIES as cat}
          <li>
            <button
              onclick={() => selectCategory(cat.value)}
              class="w-full flex items-center gap-stack-md p-stack-md mx-stack-sm rounded-full cursor-pointer text-left transition-all {selectedCategory === cat.value ? 'bg-secondary-container text-on-secondary-container font-bold' : 'text-on-surface-variant hover:bg-surface-container-high'}"
            >
              <span class="material-symbols-outlined">{cat.icon}</span>
              {cat.name}
            </button>
          </li>
        {/each}
      </ul>
    </nav>
  </aside>

  <!-- Main Content Area -->
  <main class="flex-1 flex flex-col md:ml-64 relative min-h-screen pb-20 md:pb-0">
    <!-- TopAppBar -->
    <header class="w-full top-0 sticky bg-surface dark:bg-inverse-surface border-b border-outline-variant dark:border-outline z-30">
      <div class="flex justify-between items-center px-margin-desktop py-stack-md max-w-container-max mx-auto">
        <button class="flex items-center gap-stack-md cursor-pointer active:opacity-80 md:hidden border-none bg-transparent" onclick={onNavigateToCatalog} aria-label="Go to catalog">
          <span class="material-symbols-outlined text-secondary dark:text-secondary-fixed">clinical_notes</span>
          <span class="font-headline-md text-headline-md font-bold text-primary dark:text-on-primary-fixed">EpiProtocol</span>
        </button>
        <div class="hidden md:flex items-center w-full max-w-2xl mx-auto px-4">
          <!-- Search Bar Component -->
          <div class="relative w-full group">
            <span class="absolute inset-y-0 left-0 flex items-center pl-3">
              <span class="material-symbols-outlined text-outline">search</span>
            </span>
            <input
              class="w-full bg-surface-container-lowest border border-outline-variant rounded-full py-3 pl-10 pr-12 focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary text-body-md transition-colors"
              placeholder="Search epidemiological protocols..."
              type="text"
              value={searchQuery}
              oninput={handleSearchInput}
            />
            <button class="absolute inset-y-0 right-0 flex items-center pr-3 text-outline hover:text-secondary transition-colors" aria-label="Filters options toggle">
              <span class="material-symbols-outlined">tune</span>
            </button>
          </div>
        </div>
        <div class="flex items-center cursor-pointer active:opacity-80" aria-label="Account Profile">
          <span class="material-symbols-outlined text-secondary dark:text-secondary-fixed text-[28px]">account_circle</span>
        </div>
      </div>
    </header>

    <!-- Mobile Search Bar Component -->
    <div class="md:hidden px-margin-mobile py-stack-sm bg-surface sticky top-[73px] z-20 border-b border-outline-variant">
      <div class="relative w-full">
        <span class="absolute inset-y-0 left-0 flex items-center pl-3">
          <span class="material-symbols-outlined text-outline">search</span>
        </span>
        <input
          class="w-full bg-surface-container-lowest border border-outline-variant rounded-full py-2 pl-10 pr-10 focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary text-body-md transition-colors"
          placeholder="Search..."
          type="text"
          value={searchQuery}
          oninput={handleSearchInput}
        />
        <button class="absolute inset-y-0 right-0 flex items-center pr-3 text-outline hover:text-secondary transition-colors" aria-label="Mobile tune button">
          <span class="material-symbols-outlined">tune</span>
        </button>
      </div>
    </div>

    <!-- Inner Content -->
    <div class="max-w-container-max mx-auto w-full px-margin-mobile md:px-margin-desktop py-stack-lg flex-1">
      <!-- Quick Filters -->
      <div class="mb-stack-lg flex flex-wrap gap-stack-sm items-center">
        <span class="font-label-md text-label-md text-on-surface-variant mr-2">Quick Filters:</span>
        <button
          onclick={() => toggleQuickFilter("Respiratory")}
          class="px-4 py-1.5 rounded-full font-label-md text-label-md border transition-colors flex items-center gap-1 {selectedCategory === 'Respiratory' ? 'bg-secondary-container text-on-secondary-container border-transparent' : 'bg-surface-container-lowest text-on-surface border-outline-variant hover:bg-surface-container-high'}"
        >
          {#if selectedCategory === "Respiratory"}
            <span class="material-symbols-outlined text-[16px]">close</span>
          {/if}
          Respiratory
        </button>
        <button
          onclick={() => toggleQuickFilter("Zoonotic")}
          class="px-4 py-1.5 rounded-full font-label-md text-label-md border transition-colors flex items-center gap-1 {selectedCategory === 'Zoonotic' ? 'bg-secondary-container text-on-secondary-container border-transparent' : 'bg-surface-container-lowest text-on-surface border-outline-variant hover:bg-surface-container-high'}"
        >
          {#if selectedCategory === "Zoonotic"}
            <span class="material-symbols-outlined text-[16px]">close</span>
          {/if}
          Zoonotic
        </button>
        <button
          onclick={() => toggleQuickFilter("Enteric")}
          class="px-4 py-1.5 rounded-full font-label-md text-label-md border transition-colors flex items-center gap-1 {selectedCategory === 'Enteric' ? 'bg-secondary-container text-on-secondary-container border-transparent' : 'bg-surface-container-lowest text-on-surface border-outline-variant hover:bg-surface-container-high'}"
        >
          {#if selectedCategory === "Enteric"}
            <span class="material-symbols-outlined text-[16px]">close</span>
          {/if}
          Enteric
        </button>
        <button
          onclick={toggleActiveFilter}
          class="px-4 py-1.5 rounded-full font-label-md text-label-md border transition-colors flex items-center gap-1 {selectedStatus === 'Analyzed' ? 'bg-secondary-container text-on-secondary-container border-transparent' : 'bg-surface-container-lowest text-on-surface border-outline-variant hover:bg-surface-container-high'}"
        >
          {#if selectedStatus === "Analyzed"}
            <span class="material-symbols-outlined text-[16px]">close</span>
          {/if}
          Active Only
        </button>
      </div>

      <div class="mb-stack-md flex justify-between items-end">
        <h2 class="font-headline-md text-headline-md font-semibold text-on-surface" id="results-count">
          {totalItems} {totalItems === 1 ? 'Result' : 'Results'} for "{searchQuery}"
        </h2>
        <span class="font-label-md text-label-md text-on-surface-variant">Sort by: Relevance</span>
      </div>

      <!-- Results Grid -->
      <div class="grid grid-cols-1 gap-gutter">
        {#if items.length === 0}
          <div class="bg-surface-container-lowest rounded-lg border border-outline-variant p-stack-xl text-center">
            <span class="material-symbols-outlined text-[48px] text-outline mb-2">search_off</span>
            <p class="font-headline-sm font-semibold text-on-surface mb-1">No protocols found</p>
            <p class="text-body-md text-on-surface-variant">Try refining your search terms or filters.</p>
          </div>
        {:else}
          {#each items as item}
            <!-- Card -->
            <div
              onclick={() => onSelectMaterial(item)}
              onkeydown={(e) => { if (e.key === "Enter" || e.key === " ") onSelectMaterial(item); }}
              tabindex="0"
              role="button"
              class="bg-surface-container-lowest rounded-lg border border-outline-variant p-stack-md hover:shadow-md transition-shadow cursor-pointer flex flex-col md:flex-row gap-stack-md relative overflow-hidden group focus-visible:ring-2 focus-visible:ring-secondary focus-visible:outline-none text-left"
              aria-label="View protocol details for {item.title}"
            >
              <div class="absolute left-0 top-0 bottom-0 w-1 {getLeftBarClasses(item.status)} rounded-l-lg"></div>
              <div class="flex-1 pl-2">
                <div class="flex justify-between items-start mb-2">
                  <h3 class="font-headline-sm text-headline-sm text-on-surface font-semibold group-hover:text-secondary transition-colors">
                    {item.title}
                  </h3>
                  <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium font-label-sm border {getBadgeClasses(item.status)}">
                    {item.status === 'Analyzed' ? 'Active' : item.status}
                  </span>
                </div>
                <p class="font-body-md text-body-md text-on-surface-variant mb-4 line-clamp-2">
                  {item.content || "No description provided."}
                </p>
                <div class="flex flex-wrap gap-4 items-center mt-auto">
                  <div class="flex items-center gap-1 text-on-surface-variant font-label-md text-label-md">
                    <span class="material-symbols-outlined text-[16px]">calendar_today</span>
                    Updated: {formatDate(item.updated_at)}
                  </div>
                  <div class="flex items-center gap-1 text-on-surface-variant font-label-md text-label-md">
                    <span class="material-symbols-outlined text-[16px]">person</span>
                    Dr. Sarah Chen
                  </div>
                  <div class="flex items-center gap-1 text-on-surface-variant font-label-md text-label-md">
                    <span class="material-symbols-outlined text-[16px]">tag</span>
                    EP-{item.id}
                  </div>
                </div>
              </div>
            </div>
          {/each}
        {/if}
      </div>

      <!-- Pagination -->
      {#if totalPages > 1}
        <div class="mt-stack-lg flex justify-center items-center gap-2">
          <button
            onclick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
            class="p-2 rounded-full border border-outline-variant text-outline hover:text-on-surface hover:bg-surface-container-high transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="Previous page"
          >
            <span class="material-symbols-outlined">chevron_left</span>
          </button>

          {#each Array(totalPages) as _, i}
            <button
              onclick={() => handlePageChange(i + 1)}
              class="w-8 h-8 rounded-full font-label-md text-label-md flex items-center justify-center transition-colors {currentPage === i + 1 ? 'bg-secondary text-on-secondary font-bold' : 'hover:bg-surface-container-high text-on-surface'}"
            >
              {i + 1}
            </button>
          {/each}

          <button
            onclick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            class="p-2 rounded-full border border-outline-variant text-on-surface hover:bg-surface-container-high transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="Next page"
          >
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      {/if}
    </div>
  </main>

  <!-- BottomNavBar (Mobile) -->
  <nav class="md:hidden fixed bottom-0 w-full z-50 bg-surface dark:bg-inverse-surface border-t border-outline-variant shadow-md flex justify-around items-center h-16 px-margin-mobile text-secondary dark:text-secondary-fixed font-label-sm text-label-sm-mobile">
    <button class="flex flex-col items-center justify-center bg-secondary-container text-on-secondary-container rounded-xl p-2 transition-transform active:scale-95" aria-label="Mobile Search page link active">
      <span class="material-symbols-outlined">search</span>
      Search
    </button>
    <button onclick={onNavigateToCatalog} class="flex flex-col items-center justify-center text-on-surface-variant p-2 transition-transform active:scale-95" aria-label="Mobile Catalog page link">
      <span class="material-symbols-outlined">inventory_2</span>
      Catalog
    </button>
    <button onclick={onNavigateToCreate} class="flex flex-col items-center justify-center text-on-surface-variant p-2 transition-transform active:scale-95" aria-label="Mobile Create page link">
      <span class="material-symbols-outlined">add_box</span>
      Add New
    </button>
  </nav>
</div>

<style>
  /* Extra focus outline behavior and accessibility classes */
  button:focus-visible, input:focus-visible {
    outline: 2px solid #0058be !important;
    outline-offset: 2px !important;
  }
</style>
