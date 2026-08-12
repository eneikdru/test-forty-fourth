<script>
  // Svelte 5 state runes
  let materials = $state(loadMaterials());
  let searchQuery = $state("");
  let selectedCategories = $state([]);
  let selectedStatuses = $state([]);
  let isFormVisible = $state(false);

  // Form states
  let formTitle = $state("");
  let formContent = $state("");
  let formCategory = $state("Viral Strains");
  let formStatus = $state("Pending");
  let validationError = $state("");
  let showSuccessToast = $state(false);

  const CATEGORY_OPTIONS = ["Viral Strains", "Bacterial Cultures", "Antibodies"];
  const STATUS_OPTIONS = ["Analyzed", "Pending", "Archived", "Urgent"];

  function loadMaterials() {
    try {
      const stored = localStorage.getItem("epitrack_materials");
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (e) {
      console.error("Error reading localStorage", e);
    }
    return [
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

  function saveMaterials(updatedList) {
    try {
      localStorage.setItem("epitrack_materials", JSON.stringify(updatedList));
    } catch (e) {
      console.error("Error writing localStorage", e);
    }
  }

  // Filter and Search logic using Svelte 5 $derived rune
  let filteredMaterials = $derived.by(() => {
    return materials.filter(m => {
      // Search matches Title, ID, or Description/Content
      const query = searchQuery.trim().toLowerCase();
      const matchesSearch = query === "" ||
        m.title.toLowerCase().includes(query) ||
        m.id.toLowerCase().includes(query) ||
        (m.content && m.content.toLowerCase().includes(query));

      // Category matches
      const matchesCategory = selectedCategories.length === 0 ||
        selectedCategories.includes(m.category);

      // Status matches
      const matchesStatus = selectedStatuses.length === 0 ||
        selectedStatuses.includes(m.status);

      return matchesSearch && matchesCategory && matchesStatus;
    });
  });

  function toggleCategory(cat) {
    if (selectedCategories.includes(cat)) {
      selectedCategories = selectedCategories.filter(c => c !== cat);
    } else {
      selectedCategories = [...selectedCategories, cat];
    }
  }

  function toggleStatus(stat) {
    if (selectedStatuses.includes(stat)) {
      selectedStatuses = selectedStatuses.filter(s => s !== stat);
    } else {
      selectedStatuses = [...selectedStatuses, stat];
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
      return isoString;
    }
  }

  function openCreateForm() {
    formTitle = "";
    formContent = "";
    formCategory = "Viral Strains";
    formStatus = "Pending";
    validationError = "";
    isFormVisible = true;
  }

  function closeCreateForm() {
    isFormVisible = false;
  }

  function handleSubmitMaterial(event) {
    event.preventDefault();
    validationError = "";

    if (!formTitle.trim()) {
      validationError = "Material title is required.";
      return;
    }
    if (!formContent.trim()) {
      validationError = "Material description/content is required.";
      return;
    }

    // Generate unique serializable ID
    const randomSuffix = Math.floor(100 + Math.random() * 900);
    const categoryLetter = formCategory.charAt(0).toUpperCase();
    const newId = `EP-2023-${categoryLetter}${randomSuffix}`;

    const newMaterial = {
      id: newId,
      title: formTitle.trim(),
      content: formContent.trim(),
      category: formCategory,
      updated_at: new Date().toISOString(),
      status: formStatus
    };

    materials = [newMaterial, ...materials];
    saveMaterials(materials);

    // Toast success feedback (biosemantic success state)
    showSuccessToast = true;
    setTimeout(() => {
      showSuccessToast = false;
    }, 4000);

    isFormVisible = false;
  }
</script>

<div class="bg-background min-h-screen flex flex-col font-body-md text-on-surface">
  <!-- TopAppBar -->
  <header class="w-full top-0 sticky bg-surface-container-lowest border-b border-outline-variant flex justify-between items-center px-margin-mobile md:px-margin-desktop py-sm z-40 shadow-sm">
    <div class="flex items-center gap-4">
      <h1 class="font-headline-md text-headline-md font-bold text-primary">EpiTrack</h1>
    </div>
    <!-- Desktop Nav Items -->
    <nav class="desktop-nav gap-6 items-center flex-1 ml-12 hidden md:flex">
      <button
        class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-variant/50 transition-colors px-4 py-2 rounded-lg cursor-pointer active:opacity-70"
        onclick={() => { isFormVisible = false; searchQuery = ''; selectedCategories = []; selectedStatuses = []; }}
        aria-label="Navigate to Dashboard"
      >
        <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' 0;">dashboard</span>
        <span class="font-label-md text-label-md">Dashboard</span>
      </button>
      <button
        class="flex flex-col items-center justify-center text-primary hover:bg-surface-variant/50 transition-colors px-4 py-2 rounded-lg cursor-pointer active:opacity-70 border-l-4 border-on-tertiary-container pl-3 ml-1 bg-surface-variant/30"
        onclick={() => { isFormVisible = false; }}
        aria-label="Navigate to Catalog"
      >
        <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' 1;">inventory_2</span>
        <span class="font-label-md text-label-md">Catalog</span>
      </button>
      <button
        class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-variant/50 transition-colors px-4 py-2 rounded-lg cursor-pointer active:opacity-70"
        onclick={openCreateForm}
        aria-label="Add New Material Form"
      >
        <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' 0;">add_box</span>
        <span class="font-label-md text-label-md">Add New</span>
      </button>
      <button
        class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-variant/50 transition-colors px-4 py-2 rounded-lg cursor-pointer active:opacity-70"
        aria-label="History"
      >
        <span class="material-symbols-outlined mb-1" style="font-variation-settings: 'FILL' 0;">history</span>
        <span class="font-label-md text-label-md">History</span>
      </button>
    </nav>
    <div class="flex items-center gap-4">
      <button class="text-primary hover:bg-surface-variant/50 transition-colors p-2 rounded-full cursor-pointer active:opacity-70 flex items-center justify-center" aria-label="Notifications">
        <span class="material-symbols-outlined">notifications</span>
      </button>
      <div class="w-8 h-8 rounded-full bg-surface-variant overflow-hidden cursor-pointer flex-shrink-0">
        <img alt="User Profile" class="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAZHkCtIGLXX1WpY0zxkChj-PQ73w8ebTDUVqJxixoLdGzbYC2UhSekIdqcbLhpKBUPjSlJGsTf-fkpbMgFhNBdKazh_jxYOUPcIo8N4En3ewCMHPycxyvB_5qPP79yp4Idm-pqvlu-7Px4g8y_wYm-NdRL7hLXps9dBMlaRGuUgXYs5NgeztmXxS5mQFaxG9NARJLEW-I4KkA7x9zAVJyGTdfPWNMTOaNFERX-3k3fW07lOV2gjZNaapO0qxi73GYt-CQbOIyy6Fls"/>
      </div>
    </div>
  </header>

  <!-- Success Toast (Ruth Milliken's biosemantic feedback rule) -->
  {#if showSuccessToast}
    <div class="fixed top-20 right-4 bg-teal-50 border border-[#5EEAD4] text-[#134E4A] px-4 py-3 rounded-lg shadow-lg flex items-center gap-2 z-50 animate-bounce" role="alert">
      <span class="material-symbols-outlined text-on-tertiary-container">check_circle</span>
      <span class="font-bold">Success!</span> Material has been saved successfully.
    </div>
  {/if}

  <!-- Main Content Stage -->
  <main class="flex-1 w-full max-w-[1440px] mx-auto flex flex-col md:flex-row pb-24 md:pb-8">

    {#if !isFormVisible}
      <!-- Optional Secondary Sidebar (Filters) for Desktop -->
      <aside class="hidden md:block w-[280px] p-margin-desktop border-r border-outline-variant flex-shrink-0 min-h-[calc(100vh-64px)]">
        <h2 class="font-headline-md text-headline-md mb-6">Filters</h2>
        <div class="mb-6">
          <span class="block font-body-sm text-body-sm font-bold text-on-surface mb-2">Category</span>
          <div class="flex flex-col gap-2">
            {#each CATEGORY_OPTIONS as cat}
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  class="rounded border-outline text-secondary focus:ring-secondary focus:ring-2"
                  type="checkbox"
                  checked={selectedCategories.includes(cat)}
                  onchange={() => toggleCategory(cat)}
                />
                <span class="font-body-md text-body-md">{cat}</span>
              </label>
            {/each}
          </div>
        </div>
        <div class="mb-6">
          <span class="block font-body-sm text-body-sm font-bold text-on-surface mb-2">Status</span>
          <div class="flex flex-col gap-2">
            {#each STATUS_OPTIONS as stat}
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  class="rounded border-outline text-secondary focus:ring-secondary focus:ring-2"
                  type="checkbox"
                  checked={selectedStatuses.includes(stat)}
                  onchange={() => toggleStatus(stat)}
                />
                <span class="font-body-md text-body-md">{stat}</span>
              </label>
            {/each}
          </div>
        </div>

        <button
          class="mt-4 w-full bg-secondary text-on-primary font-body-md font-bold py-3 px-4 rounded-xl shadow-md hover:bg-[#004395] transition-all focus:ring-2 focus:ring-[#001a42] focus:outline-none flex items-center justify-center gap-2"
          onclick={openCreateForm}
          id="btn-create-desktop"
        >
          <span class="material-symbols-outlined">add</span> Create Material
        </button>
      </aside>

      <!-- Data Stage -->
      <div class="flex-1 p-margin-mobile md:p-margin-desktop flex flex-col gap-xl">
        <!-- Top Controls (Search & Chips) -->
        <div class="flex flex-col lg:flex-row gap-4 items-start lg:items-center justify-between bg-surface-container-lowest p-4 rounded-xl border border-[#E2E8F0] shadow-sm">
          <div class="relative w-full lg:w-96">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
            <input
              class="w-full pl-10 pr-4 py-2 bg-surface-container-lowest border border-[#CBD5E1] rounded-lg font-body-md text-body-md focus:outline-none focus:border-secondary focus:ring-2 focus:ring-secondary transition-colors"
              placeholder="Search Material ID, Name or Content..."
              type="text"
              bind:value={searchQuery}
            />
          </div>

          <!-- Mobile Create Button (Visible on mobile instead of desktop sidebar) -->
          <div class="w-full md:hidden">
            <button
              class="w-full bg-secondary text-on-primary font-body-md font-bold py-3 px-4 rounded-xl shadow-md hover:bg-[#004395] transition-all focus:ring-2 focus:ring-[#001a42] focus:outline-none flex items-center justify-center gap-2"
              onclick={openCreateForm}
              id="btn-create-mobile"
            >
              <span class="material-symbols-outlined">add</span> Create Material
            </button>
          </div>

          <!-- Mobile Filter Chips (Horizontal Scroll) -->
          <div class="flex md:hidden gap-2 overflow-x-auto w-full pb-2 snap-x">
            <div class="snap-start flex-shrink-0 px-3 py-1.5 rounded-full border border-outline-variant bg-surface-container-lowest font-label-md text-label-md text-on-surface-variant whitespace-nowrap flex items-center gap-1">
              Category Filters Active: {selectedCategories.length || "All"}
            </div>
            <div class="snap-start flex-shrink-0 px-3 py-1.5 rounded-full border border-outline-variant bg-surface-container-lowest font-label-md text-label-md text-on-surface-variant whitespace-nowrap flex items-center gap-1">
              Status Filters Active: {selectedStatuses.length || "All"}
            </div>
          </div>
        </div>

        <!-- Complex Data Table -->
        <div class="bg-surface-container-lowest rounded-xl border border-[#E2E8F0] overflow-hidden shadow-sm flex-1 min-h-[300px]">
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse min-w-[600px]" id="materials-table">
              <thead>
                <tr class="bg-surface-container-low border-b border-[#E2E8F0]">
                  <th class="font-label-md text-label-md text-on-surface px-4 py-3 sticky left-0 bg-surface-container-low">Material ID</th>
                  <th class="font-label-md text-label-md text-on-surface px-4 py-3">Name</th>
                  <th class="font-label-md text-label-md text-on-surface px-4 py-3">Category</th>
                  <th class="font-label-md text-label-md text-on-surface px-4 py-3">Date Added</th>
                  <th class="font-label-md text-label-md text-on-surface px-4 py-3">Status</th>
                  <th class="font-label-md text-label-md text-on-surface px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="font-body-md text-body-md">
                {#if filteredMaterials.length === 0}
                  <tr>
                    <td colspan="6" class="px-4 py-12 text-center text-on-surface-variant">
                      <span class="material-symbols-outlined text-[48px] text-outline mb-2">inventory_2</span>
                      <p class="font-bold">No materials found matching your criteria.</p>
                      <button onclick={() => { searchQuery = ''; selectedCategories = []; selectedStatuses = []; }} class="text-secondary hover:underline mt-2 font-medium">Clear all filters</button>
                    </td>
                  </tr>
                {:else}
                  {#each filteredMaterials as item}
                    <tr class="data-table-row border-b border-[#E2E8F0] hover:bg-[#EFF6FF] transition-colors">
                      <td class="px-4 py-3 font-code-md text-code-md text-secondary sticky left-0 bg-inherit">{item.id}</td>
                      <td class="px-4 py-3 font-medium">
                        <div class="font-bold text-on-surface">{item.title}</div>
                        <div class="text-xs text-on-surface-variant line-clamp-1">{item.content}</div>
                      </td>
                      <td class="px-4 py-3 text-on-surface-variant">{item.category}</td>
                      <td class="px-4 py-3 text-on-surface-variant">{formatDate(item.updated_at)}</td>
                      <td class="px-4 py-3">
                        {#if item.status === 'Analyzed'}
                          <span class="inline-flex items-center px-2.5 py-1 rounded-full font-label-md text-[11px] font-bold bg-[#e0f2f1] text-[#004d40]">Analyzed</span>
                        {:else}
                          <span
                            class="inline-flex items-center px-2.5 py-1 rounded-full font-label-md text-[11px] font-bold"
                            class:bg-slate-200={item.status === 'Pending'}
                            class:text-slate-700={item.status === 'Pending'}
                            class:bg-slate-600={item.status === 'Archived'}
                            class:text-white={item.status === 'Archived'}
                            class:bg-[#fee2e2]={item.status === 'Urgent'}
                            class:text-[#ba1a1a]={item.status === 'Urgent'}
                          >
                            {item.status}
                          </span>
                        {/if}
                      </td>
                      <td class="px-4 py-3 text-right">
                        <button class="text-on-surface-variant hover:text-primary transition-colors p-1" aria-label="More Options">
                          <span class="material-symbols-outlined text-[20px]">more_vert</span>
                        </button>
                      </td>
                    </tr>
                  {/each}
                {/if}
              </tbody>
            </table>
          </div>
          <!-- Pagination Footer -->
          <div class="px-4 py-3 border-t border-[#E2E8F0] flex items-center justify-between bg-surface-container-lowest">
            <span class="font-body-sm text-body-sm text-on-surface-variant">
              Showing {filteredMaterials.length} of {materials.length} entries
            </span>
            <div class="flex gap-2">
              <button class="p-1 rounded hover:bg-surface-variant text-on-surface-variant transition-colors disabled:opacity-50" disabled aria-label="Previous Page">
                <span class="material-symbols-outlined">chevron_left</span>
              </button>
              <button class="p-1 rounded hover:bg-surface-variant text-on-surface-variant transition-colors disabled:opacity-50" disabled aria-label="Next Page">
                <span class="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    {:else}
      <!-- Create Material Form View (Full Screen / Content Transition to avoid CLS and focus attention) -->
      <div class="flex-1 p-margin-mobile md:p-margin-desktop flex flex-col gap-xl">
        <div class="max-w-2xl mx-auto w-full bg-surface-container-lowest p-6 md:p-8 rounded-2xl border border-[#E2E8F0] shadow-md transition-all">

          <div class="flex items-center justify-between mb-6 border-b border-[#E2E8F0] pb-4">
            <h2 class="font-headline-lg text-headline-lg font-bold text-primary flex items-center gap-2">
              <span class="material-symbols-outlined text-secondary">add_box</span>
              Create Epidemiological Material
            </h2>
            <button
              onclick={closeCreateForm}
              class="p-2 text-outline hover:text-primary hover:bg-[#F1F5F9] rounded-full transition-colors focus:ring-2 focus:ring-secondary focus:outline-none"
              aria-label="Cancel and Close Form"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form onsubmit={handleSubmitMaterial} class="flex flex-col gap-6" id="material-entry-form" novalidate>

            {#if validationError}
              <div class="bg-[#fee2e2] border border-[#fca5a5] text-[#ba1a1a] px-4 py-3 rounded-lg flex items-center gap-2 font-bold animate-pulse" role="alert">
                <span class="material-symbols-outlined">error</span>
                <span>{validationError}</span>
              </div>
            {/if}

            <div class="flex flex-col gap-2">
              <label for="material-title" class="font-body-sm text-body-sm font-bold text-on-surface flex items-center gap-1">
                Material Title <span class="text-error">*</span>
              </label>
              <input
                id="material-title"
                type="text"
                class="w-full px-4 py-3 rounded-lg border border-[#CBD5E1] bg-surface-container-lowest font-body-md text-body-md focus:ring-2 focus:ring-secondary focus:border-secondary focus:outline-none transition-all"
                class:border-error={validationError && !formTitle.trim()}
                placeholder="e.g. SARS-CoV-2 Standard Reference Standard"
                bind:value={formTitle}
                required
                aria-required="true"
              />
            </div>

            <div class="flex flex-col gap-2">
              <label for="material-content" class="font-body-sm text-body-sm font-bold text-on-surface flex items-center gap-1">
                Content / Description <span class="text-error">*</span>
              </label>
              <textarea
                id="material-content"
                rows="5"
                class="w-full px-4 py-3 rounded-lg border border-[#CBD5E1] bg-surface-container-lowest font-body-md text-body-md focus:ring-2 focus:ring-secondary focus:border-secondary focus:outline-none transition-all resize-y"
                class:border-error={validationError && !formContent.trim()}
                placeholder="Provide standard operating procedures, sampling protocols, or general detailed metadata of the material..."
                bind:value={formContent}
                required
                aria-required="true"
              ></textarea>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="flex flex-col gap-2">
                <label for="material-category" class="font-body-sm text-body-sm font-bold text-on-surface">Category</label>
                <select
                  id="material-category"
                  class="w-full px-4 py-3 rounded-lg border border-[#CBD5E1] bg-surface-container-lowest font-body-md text-body-md focus:ring-2 focus:ring-secondary focus:border-secondary focus:outline-none transition-all"
                  bind:value={formCategory}
                >
                  {#each CATEGORY_OPTIONS as cat}
                    <option value={cat}>{cat}</option>
                  {/each}
                </select>
              </div>

              <div class="flex flex-col gap-2">
                <label for="material-status" class="font-body-sm text-body-sm font-bold text-on-surface">Status</label>
                <select
                  id="material-status"
                  class="w-full px-4 py-3 rounded-lg border border-[#CBD5E1] bg-surface-container-lowest font-body-md text-body-md focus:ring-2 focus:ring-secondary focus:border-secondary focus:outline-none transition-all"
                  bind:value={formStatus}
                >
                  {#each STATUS_OPTIONS as stat}
                    <option value={stat}>{stat}</option>
                  {/each}
                </select>
              </div>
            </div>

            <div class="flex items-center justify-end gap-4 mt-4 pt-6 border-t border-[#E2E8F0]">
              <button
                type="button"
                onclick={closeCreateForm}
                class="px-6 py-3 rounded-xl border border-outline-variant text-on-surface font-body-md font-bold hover:bg-[#F1F5F9] focus:ring-2 focus:ring-secondary focus:outline-none transition-all"
              >
                Cancel
              </button>
              <button
                type="submit"
                class="px-8 py-3 bg-secondary text-on-primary font-body-md font-bold rounded-xl shadow-md hover:bg-[#004395] focus:ring-2 focus:ring-[#001a42] focus:outline-none transition-all flex items-center gap-2"
                id="btn-submit-material"
              >
                <span class="material-symbols-outlined">save</span>
                Save Material
              </button>
            </div>

          </form>
        </div>
      </div>
    {/if}

  </main>

  <!-- BottomNavBar (Mobile Only) -->
  <nav class="mobile-nav fixed bottom-0 w-full z-50 bg-surface-container-lowest flex justify-around items-center h-16 px-2 border-t border-outline-variant md:hidden shadow-[0_-4px_12px_rgba(0,0,0,0.05)]">
    <button
      class="flex flex-col items-center justify-center px-4 py-1 hover:bg-surface-variant transition-all scale-95 active:scale-90 w-1/4"
      class:text-secondary={!isFormVisible}
      class:text-on-surface-variant={isFormVisible}
      onclick={() => { isFormVisible = false; searchQuery = ''; selectedCategories = []; selectedStatuses = []; }}
      aria-label="Mobile Catalog Dashboard"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {!isFormVisible ? 1 : 0};">inventory_2</span>
      <span class="font-label-md text-label-md mt-1">Catalog</span>
    </button>
    <button
      class="flex flex-col items-center justify-center px-4 py-1 hover:bg-surface-variant transition-all scale-95 active:scale-90 w-1/4"
      class:text-secondary={isFormVisible}
      class:text-on-surface-variant={!isFormVisible}
      onclick={openCreateForm}
      aria-label="Mobile Add New Form"
      id="mobile-nav-add-new"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {isFormVisible ? 1 : 0};">add_box</span>
      <span class="font-label-md text-label-md mt-1">Add New</span>
    </button>
    <button
      class="flex flex-col items-center justify-center text-on-surface-variant px-4 py-1 hover:bg-surface-variant transition-all scale-95 active:scale-90 w-1/4"
      aria-label="Mobile History"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 0;">history</span>
      <span class="font-label-md text-label-md mt-1">History</span>
    </button>
  </nav>
</div>

<style>
  /* Base custom styling extending Tailwind styles */
  .data-table-row {
    transition: background-color 0.2s ease;
  }

  /* Focus-visible ring styles for interactive controls */
  button:focus-visible, input:focus-visible, select:focus-visible, textarea:focus-visible {
    outline: 2px solid #0058be !important;
    outline-offset: 2px !important;
  }
</style>
